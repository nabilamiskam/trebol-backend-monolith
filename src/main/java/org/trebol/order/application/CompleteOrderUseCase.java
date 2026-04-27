package org.trebol.order.application;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.trebol.api.models.OrderPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderStatus;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.services.crud.OrdersCrudService;
import org.trebol.order.domain.InvalidOrderTransitionException;
import org.trebol.order.domain.OrderStatusCode;
import org.trebol.order.domain.OrderWorkflow;

@Service
public class CompleteOrderUseCase {
    private static final String THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION =
        "The transaction is not in a valid state for this api";

    private final OrdersCrudService crudService;
    private final OrdersRepository ordersRepository;
    private final OrderStatusesRepository orderStatusesRepository;
    private final OrderWorkflow workflow = new OrderWorkflow();

    public CompleteOrderUseCase(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderStatusesRepository orderStatusesRepository
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderStatusesRepository = orderStatusesRepository;
    }

    public Order complete(OrderPojo sell) throws BadInputException, EntityNotFoundException {
        Order existingOrder = crudService.getExisting(sell)
            .orElseThrow(() -> new EntityNotFoundException("No transaction matches given input"));

        OrderStatusCode current = OrderStatusCode.fromCode(existingOrder.getStatus().getCode());

        final OrderStatusCode next;
        try {
            next = workflow.complete(current); // should yield DELIVERY_COMPLETE / COMPLETED
        } catch (InvalidOrderTransitionException ex) {
            throw new BadInputException(THE_TRANSACTION_IS_NOT_IN_A_VALID_STATE_FOR_THIS_OPERATION);
        }

        OrderStatus nextStatus = orderStatusesRepository.findByCode(next.code())
            .orElseThrow(() -> new IllegalStateException("No status matches code: " + next.code()));

        ordersRepository.setStatus(existingOrder.getId(), nextStatus);
        return existingOrder;
    }
}