package org.trebol.order.domain;

import java.util.Set;

import org.trebol.order.application.OrderTransitionCommand;

public class InvalidOrderTransitionException extends RuntimeException {
    private final OrderTransitionCommand command;
    private final OrderStatusCode from;
    private final Set<OrderStatusCode> allowedFrom;

    public InvalidOrderTransitionException(
        OrderTransitionCommand command,
        OrderStatusCode from,
        Set<OrderStatusCode> allowedFrom
    ) {
        super("Invalid order transition for command " + command
            + ": current=" + from
            + ", allowedFrom=" + allowedFrom);
        this.command = command;
        this.from = from;
        this.allowedFrom = allowedFrom;
    }

    public OrderTransitionCommand getCommand() {
        return command;
    }

    public OrderStatusCode getFrom() {
        return from;
    }

    public Set<OrderStatusCode> getAllowedFrom() {
        return allowedFrom;
    }
}