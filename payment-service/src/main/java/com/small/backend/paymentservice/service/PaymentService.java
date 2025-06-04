package com.small.backend.paymentservice.service;

import com.small.backend.paymentservice.entity.Payment;
import com.small.backend.paymentservice.entity.PaymentMethod;
import dto.PaymentOrderResponse;
import dto.OrderPaymentDto;

import java.util.UUID;

public interface PaymentService {
    PaymentOrderResponse createPayment(OrderPaymentDto orderPaymentDto);
    Payment getPaymentById(UUID paymentId);
    Payment getPaymentByOrderId(UUID orderId);
    Payment submitPayment(UUID paymentId, PaymentMethod paymentMethod);
    Payment cancelByOrderId(UUID orderId);
    Payment refundByOrderId(UUID orderId, Double refundPrice);
}
