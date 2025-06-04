package com.small.backend.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;
import java.util.UUID;

public class OrderRefundDto {
    @NotEmpty
    private Map<UUID, @NotNull @Positive Integer> items;

    public Map<UUID, Integer> getItems() {
        return items;
    }

    public void setItems(Map<UUID, Integer> items) {
        this.items = items;
    }
}
