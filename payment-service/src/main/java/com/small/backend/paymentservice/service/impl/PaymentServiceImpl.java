package com.small.backend.paymentservice.service.impl;

import com.small.backend.paymentservice.dao.PaymentRepository;
import com.small.backend.paymentservice.entity.Payment;
import com.small.backend.paymentservice.entity.PaymentMethod;
import com.small.backend.paymentservice.entity.PaymentStatus;
import com.small.backend.paymentservice.service.PaymentService;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public String createPayment(UUID orderId, Double totalPrice) {
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.CREATED);
        payment.setTotalPrice(totalPrice);
        payment.setRefundPrice(0.0);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD); // initial value

        Payment savedPayment = paymentRepository.save(payment);

        return savedPayment.getId().toString();
    }

    @Override
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElseThrow(
                () -> new ResourceNotFoundException("Payment " + paymentId + " not found")
        );
    }

    @Override
    public Payment getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Payment for order " + orderId + " not found")
        );
    }

    @Override
    public Payment submitPayment(UUID paymentId, PaymentMethod paymentMethod) {
        Payment payment = getPaymentById(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status not in CREATED.");
        }
        payment.setPaymentMethod(paymentMethod);

        // TODO: call /orders/{orderId}/paid, roll back if fails.

        return paymentRepository.save(payment);
    }

    @Override
    public Payment cancelByOrderId(UUID orderId) {
        Payment payment = getPaymentByOrderId(orderId);
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment not SUCCESS yet.");
        }
        payment.setPaymentStatus(PaymentStatus.FULLY_REFUNDED);
        return paymentRepository.save(payment);
    }

    @Override
    public Payment refundByOrderId(UUID orderId, Double refundPrice) {
        Payment payment = getPaymentByOrderId(orderId);
        if (payment.getPaymentStatus() == PaymentStatus.FULLY_REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment already FULLY_REFUNDED.");
        } else if (payment.getPaymentStatus() == PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment not SUCCESS yet.");
        }

        double availableRefundPrice = payment.getTotalPrice() - payment.getRefundPrice();
        if (refundPrice > availableRefundPrice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund price too large.");
        }
        if (refundPrice == availableRefundPrice) {
            payment.setRefundPrice(payment.getTotalPrice());
            payment.setPaymentStatus(PaymentStatus.FULLY_REFUNDED);
        } else {
            payment.setRefundPrice(payment.getRefundPrice() + refundPrice);
            payment.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        return paymentRepository.save(payment);
    }
}
