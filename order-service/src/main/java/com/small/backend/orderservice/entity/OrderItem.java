package com.small.backend.orderservice.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.UUID;

@UserDefinedType("order_item")
public class OrderItem {
    @Column("item_id")
    private UUID itemId;

    @Column("item_name")
    private String itemName;

    @Column("quantity")
    private Integer quantity;

    @Column("refund_quantity")
    private Integer refundQuantity;

    @Column("unit_price")
    private Double unitPrice;

    @Column("merchant_id")
    private UUID merchantId;

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getRefundQuantity() {
        return refundQuantity;
    }

    public void setRefundQuantity(Integer refundQuantity) {
        this.refundQuantity = refundQuantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }
}
