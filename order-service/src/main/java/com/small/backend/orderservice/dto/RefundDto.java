package com.small.backend.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;
import java.util.UUID;

public class RefundDto {
    @NotNull
    private UUID orderId;

    @NotEmpty
    private Map<UUID, @NotNull @Positive Integer> items;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Map<UUID, Integer> getItems() {
        return items;
    }

    public void setItems(Map<UUID, Integer> items) {
        this.items = items;
    }
}
