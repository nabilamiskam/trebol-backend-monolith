package org.trebol.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderStatus;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.services.crud.OrdersCrudService;

import jakarta.persistence.EntityNotFoundException;

class FailPaymentUseCaseTest {

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;

    private FailPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);
        useCase = new FailPaymentUseCase(crudService, ordersRepository, orderStatusesRepository);
    }

    @Test
    void failPayment_whenPaymentStarted_updatesStatusToPaymentFailed() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(2) // PAYMENT_STARTED
            .name("Payment Started")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        OrderStatus failedStatusEntity = OrderStatus.builder()
            .code(-2) // PAYMENT_FAILED
            .name("Payment Failed")
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(orderStatusesRepository.findByCode(-2)).thenReturn(Optional.of(failedStatusEntity));

        Order result = useCase.failPayment(input);

        assertSame(existing, result);
        verify(ordersRepository).setStatus(10L, failedStatusEntity);
    }

    @Test
    void failPayment_whenInvalidTransition_throwsBadInputException() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(1) // PENDING (fail should not be allowed from here)
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));

        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.failPayment(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

    @Test
    void failPayment_whenOrderNotFound_throwsEntityNotFoundException() throws Exception {
        OrderPojo input = new OrderPojo();
        when(crudService.getExisting(input)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.failPayment(input));
    }
}