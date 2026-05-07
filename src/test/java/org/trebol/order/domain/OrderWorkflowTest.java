package org.trebol.order.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.trebol.order.application.OrderTransitionCommand;

class OrderWorkflowTest {

    private final OrderWorkflow workflow = new OrderWorkflow();

    static Stream<Arguments> allowedTransitions() {
        return Stream.of(
            Arguments.of(OrderTransitionCommand.START_PAYMENT, OrderStatusCode.PENDING, OrderStatusCode.PAYMENT_STARTED),
            Arguments.of(OrderTransitionCommand.MARK_PAID, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAID_UNCONFIRMED),
            Arguments.of(OrderTransitionCommand.CONFIRM, OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.PAID_CONFIRMED),
            Arguments.of(OrderTransitionCommand.REJECT, OrderStatusCode.PAID_UNCONFIRMED, OrderStatusCode.REJECTED),
            Arguments.of(OrderTransitionCommand.COMPLETE, OrderStatusCode.PAID_CONFIRMED, OrderStatusCode.DELIVERY_COMPLETE),
            Arguments.of(OrderTransitionCommand.ABORT_PAYMENT, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_CANCELLED),
            Arguments.of(OrderTransitionCommand.FAIL_PAYMENT, OrderStatusCode.PAYMENT_STARTED, OrderStatusCode.PAYMENT_FAILED)
        );
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void returns_expected_next_state(OrderTransitionCommand cmd, OrderStatusCode from, OrderStatusCode to) {
        assertEquals(to, workflow.next(cmd, from));
    }

    static Stream<Arguments> rejectedTransitions() {
        return Stream.of(
            Arguments.of(OrderTransitionCommand.START_PAYMENT, OrderStatusCode.PAYMENT_STARTED),
            Arguments.of(OrderTransitionCommand.MARK_PAID, OrderStatusCode.PENDING),
            Arguments.of(OrderTransitionCommand.CONFIRM, OrderStatusCode.PAID_CONFIRMED),
            Arguments.of(OrderTransitionCommand.REJECT, OrderStatusCode.PAID_CONFIRMED),
            Arguments.of(OrderTransitionCommand.COMPLETE, OrderStatusCode.PAID_UNCONFIRMED),
            Arguments.of(OrderTransitionCommand.ABORT_PAYMENT, OrderStatusCode.PENDING),
            Arguments.of(OrderTransitionCommand.FAIL_PAYMENT, OrderStatusCode.PAID_UNCONFIRMED)
        );
    }

    @ParameterizedTest
    @MethodSource("rejectedTransitions")
    void rejects_invalid_transitions(OrderTransitionCommand cmd, OrderStatusCode from) {
        assertThrows(InvalidOrderTransitionException.class, () -> workflow.next(cmd, from));
    }
}