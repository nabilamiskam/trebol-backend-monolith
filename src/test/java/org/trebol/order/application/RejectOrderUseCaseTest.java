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

class RejectOrderUseCaseTest {

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;

    private RejectOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);
        useCase = new RejectOrderUseCase(crudService, ordersRepository, orderStatusesRepository);
    }

    @Test
    void reject_whenPaidUnconfirmed_updatesStatusToRejected() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(3) // PAID_UNCONFIRMED
            .name("Paid, Unconfirmed")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        OrderStatus rejectedStatusEntity = OrderStatus.builder()
            .code(-3) // REJECTED
            .name("Rejected")
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(orderStatusesRepository.findByCode(-3)).thenReturn(Optional.of(rejectedStatusEntity));

        Order result = useCase.reject(input);

        assertSame(existing, result);
        verify(ordersRepository).setStatus(10L, rejectedStatusEntity);
    }

    @Test
    void reject_whenInvalidTransition_throwsBadInputException() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(1) // PENDING (reject should not be allowed from here)
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));

        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.reject(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

    @Test
    void reject_whenOrderNotFound_throwsEntityNotFoundException() throws Exception {
        OrderPojo input = new OrderPojo();
        when(crudService.getExisting(input)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.reject(input));
    }
}