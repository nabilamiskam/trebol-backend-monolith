package org.trebol.api.models;

public record StartPaymentResponsePojo(
    long buyOrder,
    String status,
    String token
) {}