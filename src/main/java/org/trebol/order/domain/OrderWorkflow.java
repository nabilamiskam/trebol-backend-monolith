package org.trebol.order.domain;

/**
 * Pure domain rules for order status transitions.
 *
 * This mirrors your current service logic:
 * - start payment:     Pending -> Payment Started
 * - mark paid:         Payment Started -> Paid, Unconfirmed
 * - confirm:           Paid, Unconfirmed -> Paid, Confirmed
 * - reject:            Paid, Unconfirmed -> Rejected
 * - complete:          Paid, Confirmed -> Delivery Complete
 * - abort:             Payment Started -> Payment Cancelled
 * - fail:              Payment Started -> Payment Failed
 *
 * Note: token persistence is not handled here; this class only validates transitions.
 */
public final class OrderWorkflow {

    public OrderStatusCode startPayment(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PENDING, OrderStatusCode.PAYMENT_STARTED);
    }

    public OrderStatusCode markPaid(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAID_UNCONFIRMED);
    }

    public OrderStatusCode confirm(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.PAID_CONFIRMED);
    }

    public OrderStatusCode reject(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.REJECTED);
    }

    public OrderStatusCode complete(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAID_CONFIRMED, OrderStatusCode.DELIVERY_COMPLETE);
    }

    public OrderStatusCode abortPayment(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_CANCELLED);
    }

    public OrderStatusCode failPayment(OrderStatusCode current) {
        return requireTransition(current, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_FAILED);
    }

    private OrderStatusCode requireTransition(
        OrderStatusCode current,
        OrderStatusCode requiredFrom,
        OrderStatusCode to
    ) {
        if (current != requiredFrom) {
            throw new InvalidOrderTransitionException(current, to);
        }
        return to;
    }
}