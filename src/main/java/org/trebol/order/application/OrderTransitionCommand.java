package org.trebol.order.application;

public enum OrderTransitionCommand {
    START_PAYMENT,
    MARK_PAID,
    CONFIRM,
    REJECT,
    COMPLETE,
    ABORT_PAYMENT,
    FAIL_PAYMENT
}