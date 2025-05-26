package com.small.backend.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class RefundRequest {
    @NotNull
    private UUID orderId;

    @NotEmpty
    private List<@NotNull RefundItemDto> items;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<RefundItemDto> getItems() {
        return items;
    }

    public void setItems(List<RefundItemDto> items) {
        this.items = items;
    }
}
