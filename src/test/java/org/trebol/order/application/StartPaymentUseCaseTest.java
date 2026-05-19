package org.trebol.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.order.application.ports.TokenGenerator;
import org.trebol.order.domain.InvalidOrderTransitionException;

import jakarta.persistence.EntityNotFoundException;

class StartPaymentUseCaseTest {

    private static final String TOKEN_64 =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private OrderTransitionService transitionService;
    private OrdersRepository ordersRepository;
    private TokenGenerator tokenGenerator;

    private StartPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        transitionService = mock(OrderTransitionService.class);
        ordersRepository = mock(OrdersRepository.class);
        tokenGenerator = mock(TokenGenerator.class);

        useCase = new StartPaymentUseCase(transitionService, ordersRepository, tokenGenerator);
    }

    @Test
    void startPayment_whenAllowed_transitionsStatus_thenGeneratesAndPersistsToken() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();

        Order transitioned = Order.builder()
            .id(10L)
            .build();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.START_PAYMENT)))
            .thenReturn(transitioned);

        when(tokenGenerator.generateToken()).thenReturn(TOKEN_64);

        // Act
        Order result = useCase.startPayment(input);

        // Assert
        assertSame(transitioned, result);
        assertEquals(TOKEN_64, transitioned.getTransactionToken());

        verify(transitionService).transition(same(input), eq(OrderTransitionCommand.START_PAYMENT));
        verify(tokenGenerator).generateToken();
        verify(ordersRepository).setTransactionToken(10L, TOKEN_64);
    }

    @Test
    void startPayment_whenInvalidTransition_throwsBadInputException_andDoesNotGenerateToken() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.START_PAYMENT)))
            .thenThrow(mock(InvalidOrderTransitionException.class));

        // Act + Assert
        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.startPayment(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(tokenGenerator, never()).generateToken();
        verify(ordersRepository, never()).setTransactionToken(anyLong(), anyString());
    }

    @Test
    void startPayment_whenOrderNotFound_bubblesUpEntityNotFoundException_andDoesNotGenerateToken() throws Exception {
        // Arrange
        OrderPojo input = new OrderPojo();

        when(transitionService.transition(same(input), eq(OrderTransitionCommand.START_PAYMENT)))
            .thenThrow(new EntityNotFoundException("No transaction matches given input"));

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> useCase.startPayment(input));

        verifyNoInteractions(tokenGenerator);
        verifyNoInteractions(ordersRepository);
    }
}