package org.trebol.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.order.domain.InvalidOrderTransitionException;

import jakarta.persistence.EntityNotFoundException;

class ConfirmOrderUseCaseTest {

    private OrderTransitionService transitionService;
    private ConfirmOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        transitionService = mock(OrderTransitionService.class);
        useCase = new ConfirmOrderUseCase(transitionService);
    }

    @Test
    void confirm_delegatesToTransitionServiceWithConfirmCommand() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();
        Order expected = Order.builder().id(10L).build();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.CONFIRM)))
            .thenReturn(expected);

        // Act
        Order result = useCase.confirm(input);

        // Assert
        assertSame(expected, result);
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.CONFIRM));
    }

    @Test
    void confirm_whenInvalidTransition_throwsBadInputException() {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.CONFIRM)))
        .thenThrow(mock(InvalidOrderTransitionException.class));

        // Act + Assert
        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.confirm(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.CONFIRM));
    }

    @Test
    void confirm_whenOrderNotFound_bubblesUpEntityNotFoundException() {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.CONFIRM)))
            .thenThrow(new EntityNotFoundException("No transaction matches given input"));

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> useCase.confirm(input));
    }
}