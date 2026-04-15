package org.trebol.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderWorkflowTest {

    private final OrderWorkflow workflow = new OrderWorkflow();

    // --------------------
    // 7 passing tests
    // --------------------

    @Test
    void startPayment_fromPending_toPaymentStarted() {
        var next = workflow.startPayment(OrderStatusCode.PENDING);
        assertEquals(OrderStatusCode.PAYMENT_STARTED, next);
    }

    @Test
    void markPaid_fromPaymentStarted_toPaidUnconfirmed() {
        var next = workflow.markPaid(OrderStatusCode.PAYMENT_STARTED);
        assertEquals(OrderStatusCode.PAID_UNCONFIRMED, next);
    }

    @Test
    void confirm_fromPaidUnconfirmed_toPaidConfirmed() {
        var next = workflow.confirm(OrderStatusCode.PAID_UNCONFIRMED);
        assertEquals(OrderStatusCode.PAID_CONFIRMED, next);
    }

    @Test
    void reject_fromPaidUnconfirmed_toRejected() {
        var next = workflow.reject(OrderStatusCode.PAID_UNCONFIRMED);
        assertEquals(OrderStatusCode.REJECTED, next);
    }

    @Test
    void complete_fromPaidConfirmed_toDeliveryComplete() {
        var next = workflow.complete(OrderStatusCode.PAID_CONFIRMED);
        assertEquals(OrderStatusCode.DELIVERY_COMPLETE, next);
    }

    @Test
    void abortPayment_fromPaymentStarted_toPaymentCancelled() {
        var next = workflow.abortPayment(OrderStatusCode.PAYMENT_STARTED);
        assertEquals(OrderStatusCode.PAYMENT_CANCELLED, next);
    }

    @Test
    void failPayment_fromPaymentStarted_toPaymentFailed() {
        var next = workflow.failPayment(OrderStatusCode.PAYMENT_STARTED);
        assertEquals(OrderStatusCode.PAYMENT_FAILED, next);
    }

    // --------------------
    // 7 failing tests (invalid "from" state)
    // --------------------

    @Test
    void startPayment_notFromPending_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.startPayment(OrderStatusCode.PAYMENT_STARTED)
        );
        assertEquals(OrderStatusCode.PAYMENT_STARTED, ex.getFrom());
        assertEquals(OrderStatusCode.PAYMENT_STARTED, ex.getTo());
    }

    @Test
    void markPaid_notFromPaymentStarted_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.markPaid(OrderStatusCode.PENDING)
        );
        assertEquals(OrderStatusCode.PENDING, ex.getFrom());
        assertEquals(OrderStatusCode.PAID_UNCONFIRMED, ex.getTo());
    }

    @Test
    void confirm_notFromPaidUnconfirmed_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.confirm(OrderStatusCode.PAID_CONFIRMED)
        );
        assertEquals(OrderStatusCode.PAID_CONFIRMED, ex.getFrom());
        assertEquals(OrderStatusCode.PAID_CONFIRMED, ex.getTo());
    }

    @Test
    void reject_notFromPaidUnconfirmed_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.reject(OrderStatusCode.PAID_CONFIRMED)
        );
        assertEquals(OrderStatusCode.PAID_CONFIRMED, ex.getFrom());
        assertEquals(OrderStatusCode.REJECTED, ex.getTo());
    }

    @Test
    void complete_notFromPaidConfirmed_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.complete(OrderStatusCode.PAID_UNCONFIRMED)
        );
        assertEquals(OrderStatusCode.PAID_UNCONFIRMED, ex.getFrom());
        assertEquals(OrderStatusCode.DELIVERY_COMPLETE, ex.getTo());
    }

    @Test
    void abortPayment_notFromPaymentStarted_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.abortPayment(OrderStatusCode.PENDING)
        );
        assertEquals(OrderStatusCode.PENDING, ex.getFrom());
        assertEquals(OrderStatusCode.PAYMENT_CANCELLED, ex.getTo());
    }

    @Test
    void failPayment_notFromPaymentStarted_throws() {
        var ex = assertThrows(
            InvalidOrderTransitionException.class,
            () -> workflow.failPayment(OrderStatusCode.PAID_UNCONFIRMED)
        );
        assertEquals(OrderStatusCode.PAID_UNCONFIRMED, ex.getFrom());
        assertEquals(OrderStatusCode.PAYMENT_FAILED, ex.getTo());
    }
}