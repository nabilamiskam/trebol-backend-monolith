package org.trebol.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trebol.api.models.OrderPojo;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderStatus;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.services.crud.OrdersCrudService;
import org.trebol.order.domain.InvalidOrderTransitionException;
import org.trebol.order.domain.OrderStatusCode;

import jakarta.persistence.EntityNotFoundException;

class OrderTransitionServiceTest {

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;

    private OrderTransitionService service;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);

        service = new OrderTransitionService(crudService, ordersRepository, orderStatusesRepository);
    }

    @Test
    void transition_happyPath_callsSetStatus_andUpdatesInMemoryOrder() {
        // Arrange
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(OrderStatusCode.PAID_UNCONFIRMED.code())
            .name("Paid, Unconfirmed")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(same(input))).thenReturn(Optional.of(existing));

        OrderStatus nextStatusEntity = OrderStatus.builder()
            .code(OrderStatusCode.PAID_CONFIRMED.code())
            .name("Paid, Confirmed")
            .build();

        when(orderStatusesRepository.findByCode(OrderStatusCode.PAID_CONFIRMED.code()))
            .thenReturn(Optional.of(nextStatusEntity));

        // Act
        Order result = service.transition(input, OrderTransitionCommand.CONFIRM);

        // Assert
        assertSame(existing, result);
        assertSame(nextStatusEntity, existing.getStatus(), "Service should update in-memory status");

        verify(ordersRepository).setStatus(10L, nextStatusEntity);
        verify(orderStatusesRepository).findByCode(OrderStatusCode.PAID_CONFIRMED.code());
        verify(crudService).getExisting(same(input));
        verifyNoMoreInteractions(ordersRepository, orderStatusesRepository, crudService);
    }

    @Test
    void transition_invalidTransition_bubblesUpInvalidOrderTransitionException() {
        // Arrange
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(OrderStatusCode.PENDING.code())
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(same(input))).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThrows(InvalidOrderTransitionException.class,
            () -> service.transition(input, OrderTransitionCommand.CONFIRM));

        verify(ordersRepository, never()).setStatus(anyLong(), any());
        verify(orderStatusesRepository, never()).findByCode(anyInt());
    }

    @Test
    void transition_whenOrderNotFound_throwsEntityNotFoundException() {
        // Arrange
        OrderPojo input = new OrderPojo();
        when(crudService.getExisting(same(input))).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class,
            () -> service.transition(input, OrderTransitionCommand.CONFIRM));

        verifyNoInteractions(ordersRepository);
        verifyNoInteractions(orderStatusesRepository);
    }
}