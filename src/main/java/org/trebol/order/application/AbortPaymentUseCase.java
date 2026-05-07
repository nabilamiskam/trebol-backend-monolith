package org.trebol.order.application;

import org.springframework.stereotype.Service;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderStatus;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.services.crud.OrdersCrudService;
import org.trebol.order.domain.InvalidOrderTransitionException;
import org.trebol.order.domain.OrderStatusCode;
import org.trebol.order.domain.OrderWorkflow;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AbortPaymentUseCase {
    private static final String THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION =
        "The transaction is not in a valid state for this api";

    private final OrdersCrudService crudService;
    private final OrdersRepository ordersRepository;
    private final OrderStatusesRepository orderStatusesRepository;
    private final OrderWorkflow workflow = new OrderWorkflow();

    public AbortPaymentUseCase(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderStatusesRepository orderStatusesRepository
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderStatusesRepository = orderStatusesRepository;
    }

    public Order abortPayment(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = crudService.getExisting(sell)
            .orElseThrow(() -> new EntityNotFoundException("No transaction matches given input"));

        OrderStatusCode current = OrderStatusCode.fromCode(existingOrder.getStatus().getCode());

        final OrderStatusCode next;
        try {
            next = workflow.next(OrderTransitionCommand.ABORT_PAYMENT, current); // 2 -> -1
        } catch (InvalidOrderTransitionException ex) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        OrderStatus nextStatus = orderStatusesRepository.findByCode(next.code())
            .orElseThrow(() -> new IllegalStateException("No status matches code: " + next.code()));

        ordersRepository.setStatus(existingOrder.getId(), nextStatus);

        // Note: existingOrder status field may not be updated in-memory; service sets API status string explicitly.
        return existingOrder;
    }
}