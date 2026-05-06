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

class MarkPaidUseCaseTest {

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;

    private MarkPaidUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);

        useCase = new MarkPaidUseCase(crudService, ordersRepository, orderStatusesRepository);
    }

    @Test
    void markPaid_whenPaymentStarted_updatesStatusToPaidUnconfirmed() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(2) // PAYMENT_STARTED
            .name("Payment Started")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        OrderStatus paidUnconfirmedStatusEntity = OrderStatus.builder()
            .code(3) // PAID_UNCONFIRMED
            .name("Paid Unconfirmed")
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(orderStatusesRepository.findByCode(3)).thenReturn(Optional.of(paidUnconfirmedStatusEntity));

        Order result = useCase.markPaid(input);

        assertSame(existing, result);
        verify(ordersRepository).setStatus(10L, paidUnconfirmedStatusEntity);
    }

    @Test
    void markPaid_whenInvalidTransition_throwsBadInputException() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(1) // PENDING (markPaid should not be allowed from here)
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));

        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.markPaid(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

    @Test
    void markPaid_whenOrderNotFound_throwsEntityNotFoundException() throws Exception {
        OrderPojo input = new OrderPojo();
        when(crudService.getExisting(input)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.markPaid(input));
    }
}