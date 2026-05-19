package org.trebol.order.application;

import org.springframework.stereotype.Service;
import org.trebol.api.models.OrderPojo;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderStatus;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.services.crud.OrdersCrudService;
import org.trebol.order.domain.OrderStatusCode;
import org.trebol.order.domain.OrderWorkflow;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderTransitionService {

    private final OrdersCrudService crudService;
    private final OrdersRepository ordersRepository;
    private final OrderStatusesRepository orderStatusesRepository;

    private final OrderWorkflow workflow = new OrderWorkflow();

    public OrderTransitionService(
        OrdersCrudService crudService,
        OrdersRepository ordersRepository,
        OrderStatusesRepository orderStatusesRepository
    ) {
        this.crudService = crudService;
        this.ordersRepository = ordersRepository;
        this.orderStatusesRepository = orderStatusesRepository;
    }

    public Order transition(OrderPojo input, OrderTransitionCommand command) throws EntityNotFoundException {
        Order existingOrder = crudService.getExisting(input)
            .orElseThrow(() -> new EntityNotFoundException("No transaction matches given input"));

        OrderStatusCode current = OrderStatusCode.fromCode(existingOrder.getStatus().getCode());
        OrderStatusCode next = workflow.next(command, current);

        OrderStatus nextStatus = orderStatusesRepository.findByCode(next.code())
            .orElseThrow(() -> new IllegalStateException("No status matches code: " + next.code()));

        ordersRepository.setStatus(existingOrder.getId(), nextStatus);
        existingOrder.setStatus(nextStatus);

        return existingOrder;
    }
}