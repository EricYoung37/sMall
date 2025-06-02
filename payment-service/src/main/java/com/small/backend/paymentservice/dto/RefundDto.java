package com.small.backend.paymentservice.dto;

import java.util.UUID;

public class RefundDto {
    private UUID orderId;
    private Double refundPrice;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Double getRefundPrice() {
        return refundPrice;
    }

    public void setRefundPrice(Double refundPrice) {
        this.refundPrice = refundPrice;
    }
}
