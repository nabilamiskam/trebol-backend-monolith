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

class AbortPaymentUseCaseTest {

    private OrderTransitionService transitionService;
    private AbortPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        transitionService = mock(OrderTransitionService.class);
        useCase = new AbortPaymentUseCase(transitionService);
    }

    @Test
    void abortPayment_delegatesToTransitionServiceWithAbortPaymentCommand() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();
        Order expected = Order.builder().id(10L).build();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.ABORT_PAYMENT)))
            .thenReturn(expected);

        // Act
        Order result = useCase.abortPayment(input);

        // Assert
        assertSame(expected, result);
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.ABORT_PAYMENT));
    }

    @Test
    void abortPayment_whenInvalidTransition_throwsBadInputException() {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.ABORT_PAYMENT)))
            .thenThrow(mock(InvalidOrderTransitionException.class));

        // Act + Assert
        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.abortPayment(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.ABORT_PAYMENT));
    }

    @Test
    void abortPayment_whenOrderNotFound_bubblesUpEntityNotFoundException() {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.ABORT_PAYMENT)))
            .thenThrow(new EntityNotFoundException("No transaction matches given input"));

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> useCase.abortPayment(input));
    }
}