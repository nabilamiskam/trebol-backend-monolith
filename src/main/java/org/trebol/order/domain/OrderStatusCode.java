package org.trebol.order.domain;

import java.util.Arrays;

public enum OrderStatusCode {
    // Positive (normal flow)
    PENDING(1, "Pending"),
    PAYMENT_STARTED(2, "Payment Started"),
    PAID_UNCONFIRMED(3, "Paid, Unconfirmed"),
    PAID_CONFIRMED(4, "Paid, Confirmed"),
    DELIVERY_ON_ROUTE(5, "Delivery On Route"),      // present in data.sql (optional for now)
    DELIVERY_COMPLETE(6, "Delivery Complete"),

    // Negative (failure/cancel flow)
    PAYMENT_CANCELLED(-1, "Payment Cancelled"),
    PAYMENT_FAILED(-2, "Payment Failed"),
    REJECTED(-3, "Rejected"),
    DELIVERY_CANCELLED(-4, "Delivery Cancelled"),   // present in data.sql (optional)
    DELIVERY_FAILED(-5, "Delivery Failed"),         // present in data.sql (optional)
    RETURNED(-6, "Returned");                       // present in data.sql (optional)

    private final int code;
    private final String displayName;

    OrderStatusCode(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /** Matches `order_status_code` in DB (`order_statuses.order_status_code`). */
    public int code() {
        return code;
    }

    /** Matches `order_status_name` in DB (`order_statuses.order_status_name`). */
    public String displayName() {
        return displayName;
    }

    public static OrderStatusCode fromCode(int code) {
        return Arrays.stream(values())
            .filter(v -> v.code == code)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown OrderStatusCode code: " + code));
    }

    public static OrderStatusCode fromDisplayName(String name) {
        return Arrays.stream(values())
            .filter(v -> v.displayName.equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown OrderStatusCode name: " + name));
    }
}