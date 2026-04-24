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

class ConfirmOrderUseCaseTest {

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;

    private ConfirmOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);
        useCase = new ConfirmOrderUseCase(crudService, ordersRepository, orderStatusesRepository);
    }

    @Test
    void confirm_whenPaidUnconfirmed_updatesStatusToPaidConfirmed() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo(); // only used as lookup key by crudService.getExisting

        OrderStatus currentStatus = OrderStatus.builder()
            .code(3) // PAID_UNCONFIRMED
            .name("Paid, Unconfirmed")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        OrderStatus confirmedStatusEntity = OrderStatus.builder()
            .code(4) // PAID_CONFIRMED
            .name("Paid, Confirmed")
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(orderStatusesRepository.findByCode(4)).thenReturn(Optional.of(confirmedStatusEntity));

        // Act
        Order result = useCase.confirm(input);

        // Assert
        assertSame(existing, result);
        verify(ordersRepository).setStatus(10L, confirmedStatusEntity);
    }

    @Test
    void confirm_whenInvalidTransition_throwsBadInputException() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(1) // PENDING (confirm should not be allowed from here)
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));

        // Act + Assert
        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.confirm(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

@Test
void confirm_whenOrderNotFound_throwsEntityNotFoundException() throws Exception {
    OrderPojo input = new OrderPojo();
    when(crudService.getExisting(input)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> useCase.confirm(input));
}
}