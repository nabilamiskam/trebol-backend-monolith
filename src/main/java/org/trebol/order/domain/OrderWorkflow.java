package org.trebol.order.domain;

import java.util.Map;

import org.trebol.order.application.OrderTransitionCommand;

/**
 * Pure domain rules for order status transitions (table-driven).
 * This class validates transitions and returns the next status code.
 */
public final class OrderWorkflow {

    // command -> (from -> to)
    private static final Map<OrderTransitionCommand, Map<OrderStatusCode, OrderStatusCode>> TRANSITIONS =
        Map.of(
            OrderTransitionCommand.START_PAYMENT,
                Map.of(OrderStatusCode.PENDING, OrderStatusCode.PAYMENT_STARTED),

            OrderTransitionCommand.MARK_PAID,
                Map.of(OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAID_UNCONFIRMED),

            OrderTransitionCommand.CONFIRM,
                Map.of(OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.PAID_CONFIRMED),

            OrderTransitionCommand.REJECT,
                Map.of(OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.REJECTED),

            OrderTransitionCommand.COMPLETE,
                Map.of(OrderStatusCode.PAID_CONFIRMED, OrderStatusCode.DELIVERY_COMPLETE),

            OrderTransitionCommand.ABORT_PAYMENT,
                Map.of(OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_CANCELLED),

            OrderTransitionCommand.FAIL_PAYMENT,
                Map.of(OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_FAILED)
        );

    public OrderStatusCode next(OrderTransitionCommand cmd, OrderStatusCode current) {
        Map<OrderStatusCode, OrderStatusCode> byFrom = TRANSITIONS.get(cmd);
        if (byFrom == null) {
            // Should never happen unless a developer forgets to add a table entry
            throw new IllegalArgumentException("Unknown transition command: " + cmd);
        }

        OrderStatusCode to = byFrom.get(current);
        if (to == null) {
            // Consistent invalid-transition behavior
            // (matches your current exception style)
            throw new InvalidOrderTransitionException(cmd, current, byFrom.keySet());
        }
        return to;
    }
}