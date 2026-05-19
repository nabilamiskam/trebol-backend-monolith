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

class RejectOrderUseCaseTest {

    private OrderTransitionService transitionService;
    private RejectOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        transitionService = mock(OrderTransitionService.class);
        useCase = new RejectOrderUseCase(transitionService);
    }

    @Test
    void reject_delegatesToTransitionServiceWithRejectCommand() throws Exception {
        OrderPojo input = new OrderPojo();
        Order expected = Order.builder().id(10L).build();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.REJECT)))
            .thenReturn(expected);

        Order result = useCase.reject(input);

        assertSame(expected, result);
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.REJECT));
    }

    @Test
    void reject_whenInvalidTransition_throwsBadInputException() {
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.REJECT)))
            .thenThrow(mock(InvalidOrderTransitionException.class));

        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.reject(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());
        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.REJECT));
    }

    @Test
    void reject_whenOrderNotFound_bubblesUpEntityNotFoundException() {
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.REJECT)))
            .thenThrow(new EntityNotFoundException("No transaction matches given input"));

        assertThrows(EntityNotFoundException.class, () -> useCase.reject(input));
    }
}