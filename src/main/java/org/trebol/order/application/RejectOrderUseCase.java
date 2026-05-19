package org.trebol.order.application;

import org.springframework.stereotype.Service;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.order.domain.InvalidOrderTransitionException;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RejectOrderUseCase {
    private static final String THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION =
        "The transaction is not in a valid state for this api";

    private final OrderTransitionService transitionService;

    public RejectOrderUseCase(OrderTransitionService transitionService) {
        this.transitionService = transitionService;
    }

    public Order reject(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        try {
            return transitionService.transition(sell, OrderTransitionCommand.REJECT);
        } catch (InvalidOrderTransitionException ex) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }
    }
}