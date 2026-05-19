package org.trebol.order.application;

import org.springframework.stereotype.Service;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.order.application.ports.TokenGenerator;
import org.trebol.order.domain.InvalidOrderTransitionException;

import jakarta.persistence.EntityNotFoundException;

@Service
public class StartPaymentUseCase {
    private static final String THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION =
        "The transaction is not in a valid state for this api";

    private final OrderTransitionService transitionService;
    private final OrdersRepository ordersRepository;
    private final TokenGenerator tokenGenerator;

    public StartPaymentUseCase(
        OrderTransitionService transitionService,
        OrdersRepository ordersRepository,
        TokenGenerator tokenGenerator
    ) {
        this.transitionService = transitionService;
        this.ordersRepository = ordersRepository;
        this.tokenGenerator = tokenGenerator;
    }

    public Order startPayment(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        final Order updated;
        try {
            updated = transitionService.transition(sell, OrderTransitionCommand.START_PAYMENT);
        } catch (InvalidOrderTransitionException ex) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        // token side effect stays here
        String token = tokenGenerator.generateToken();
        ordersRepository.setTransactionToken(updated.getId(), token);
        updated.setTransactionToken(token);

        return updated;
    }
}