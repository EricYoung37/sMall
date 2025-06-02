package com.small.backend.paymentservice.service;

import com.small.backend.paymentservice.entity.Payment;
import com.small.backend.paymentservice.entity.PaymentMethod;

import java.util.UUID;

public interface PaymentService {
    String createPayment(UUID orderId, Double totalPrice);
    Payment getPaymentById(UUID paymentId);
    Payment getPaymentByOrderId(UUID orderId);
    Payment submitPayment(UUID paymentId, PaymentMethod paymentMethod);
    Payment cancelByOrderId(UUID orderId);
    Payment refundByOrderId(UUID orderId, Double refundPrice);
}
