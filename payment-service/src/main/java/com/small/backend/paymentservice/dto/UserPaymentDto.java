package com.small.backend.paymentservice.dto;

import com.small.backend.paymentservice.entity.PaymentMethod;

// payment request from the client app (frontend)
public class UserPaymentDto {
    PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
