package com.small.backend.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    COMPLETED,
    CANCELLED,
    FULLY_REFUNDED,
    PARTIALLY_REFUNDED
}
