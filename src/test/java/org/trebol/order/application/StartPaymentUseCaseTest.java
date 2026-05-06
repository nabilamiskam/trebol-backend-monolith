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
import org.trebol.order.application.ports.TokenGenerator;

import jakarta.persistence.EntityNotFoundException;

class StartPaymentUseCaseTest {

    private static final String TOKEN_64 =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private OrdersCrudService crudService;
    private OrdersRepository ordersRepository;
    private OrderStatusesRepository orderStatusesRepository;
    private TokenGenerator tokenGenerator;

    private StartPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = mock(OrdersCrudService.class);
        ordersRepository = mock(OrdersRepository.class);
        orderStatusesRepository = mock(OrderStatusesRepository.class);
        tokenGenerator = mock(TokenGenerator.class);

        useCase = new StartPaymentUseCase(crudService, ordersRepository, orderStatusesRepository, tokenGenerator);
    }

    @Test
    void startPayment_whenPending_generatesToken_andUpdatesStatusToPaymentStarted() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus pendingStatus = OrderStatus.builder()
            .code(1) // PENDING
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(pendingStatus)
            .build();

        OrderStatus startedStatusEntity = OrderStatus.builder()
            .code(2) // PAYMENT_STARTED
            .name("Payment Started")
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(tokenGenerator.generateToken()).thenReturn(TOKEN_64);
        when(orderStatusesRepository.findByCode(2)).thenReturn(Optional.of(startedStatusEntity));

        Order result = useCase.startPayment(input);

        assertSame(existing, result);
        assertEquals(TOKEN_64, existing.getTransactionToken());
        assertSame(startedStatusEntity, existing.getStatus());

        verify(ordersRepository).setTransactionToken(10L, TOKEN_64);
        verify(ordersRepository).setStatus(10L, startedStatusEntity);
    }

    @Test
    void startPayment_whenInvalidTransition_throwsBadInputException() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus currentStatus = OrderStatus.builder()
            .code(2) // PAYMENT_STARTED (startPayment should not be allowed from here)
            .name("Payment Started")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(currentStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));

        BadInputException ex = assertThrows(BadInputException.class, () -> useCase.startPayment(input));
        assertEquals("The transaction is not in a valid state for this api", ex.getMessage());

        verify(tokenGenerator, never()).generateToken();
        verify(ordersRepository, never()).setTransactionToken(anyLong(), anyString());
        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

    @Test
    void startPayment_whenNextStatusNotFound_throwsIllegalStateException() throws Exception {
        OrderPojo input = new OrderPojo();

        OrderStatus pendingStatus = OrderStatus.builder()
            .code(1) // PENDING
            .name("Pending")
            .build();

        Order existing = Order.builder()
            .id(10L)
            .status(pendingStatus)
            .build();

        when(crudService.getExisting(input)).thenReturn(Optional.of(existing));
        when(tokenGenerator.generateToken()).thenReturn(TOKEN_64);
        when(orderStatusesRepository.findByCode(2)).thenReturn(Optional.empty());

        IllegalStateException ex =
            assertThrows(IllegalStateException.class, () -> useCase.startPayment(input));

        assertEquals("No status matches code: 2", ex.getMessage());

        // token may already have been generated+persisted before status lookup depending on your code order
        verify(ordersRepository).setTransactionToken(10L, TOKEN_64);
        verify(ordersRepository, never()).setStatus(anyLong(), any());
    }

    @Test
    void startPayment_whenOrderNotFound_throwsEntityNotFoundException() throws Exception {
        OrderPojo input = new OrderPojo();
        when(crudService.getExisting(input)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.startPayment(input));

        verifyNoInteractions(tokenGenerator);
        verifyNoInteractions(ordersRepository);
        verifyNoInteractions(orderStatusesRepository);
    }
}