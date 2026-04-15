package org.trebol.order.domain;

public class InvalidOrderTransitionException extends RuntimeException {
    private final OrderStatusCode from;
    private final OrderStatusCode to;

    public InvalidOrderTransitionException(OrderStatusCode from, OrderStatusCode to) {
        super("Invalid order status transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public OrderStatusCode getFrom() {
        return from;
    }

    public OrderStatusCode getTo() {
        return to;
    }
}
