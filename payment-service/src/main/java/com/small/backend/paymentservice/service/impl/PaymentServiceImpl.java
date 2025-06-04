package com.small.backend.paymentservice.service.impl;

import com.small.backend.paymentservice.dao.PaymentRepository;
import com.small.backend.paymentservice.entity.Payment;
import com.small.backend.paymentservice.entity.PaymentMethod;
import com.small.backend.paymentservice.entity.PaymentStatus;
import com.small.backend.paymentservice.kafka.KafkaPaymentProducer;
import com.small.backend.paymentservice.service.PaymentService;
import dto.PaymentOrderResponse;
import dto.OrderPaymentDto;
import exception.ResourceAlreadyExistsException;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaPaymentProducer kafkaPaymentProducer;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, KafkaPaymentProducer kafkaPaymentProducer) {
        this.paymentRepository = paymentRepository;
        this.kafkaPaymentProducer = kafkaPaymentProducer;
    }

    @Override
    public PaymentOrderResponse createPayment(OrderPaymentDto orderPaymentDto) {
        String userEmail = orderPaymentDto.getUserEmail();
        UUID orderId = orderPaymentDto.getOrderId();

        Payment payment = new Payment();
        payment.setUserEmail(userEmail);
        payment.setOrderId(orderId);
        payment.setPaymentStatus(PaymentStatus.CREATED);
        payment.setTotalPrice(orderPaymentDto.getTotalPrice());
        payment.setRefundPrice(0.0);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD); // initial value

        try {
            PaymentOrderResponse paymentOrderResponse = new PaymentOrderResponse();
            paymentOrderResponse.setUserEmail(userEmail);
            paymentOrderResponse.setOrderId(orderId);
            paymentOrderResponse.setPaymentId(paymentRepository.save(payment).getId());
            return paymentOrderResponse;
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("Payment for order " + orderId + " already exists.");
        }
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
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        try {
            PaymentOrderResponse paymentOrderResponse = new PaymentOrderResponse();
            paymentOrderResponse.setUserEmail(payment.getUserEmail());
            paymentOrderResponse.setOrderId(payment.getOrderId());
            paymentOrderResponse.setPaymentId(payment.getId());
            kafkaPaymentProducer.sendPaymentSubmit(paymentOrderResponse).get();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to notify order service", e);
        }

        return paymentRepository.save(payment);
    }

    @Override
    public Payment cancelByOrderId(UUID orderId) {
        Payment payment = getPaymentByOrderId(orderId);
        if (payment.getPaymentStatus() == PaymentStatus.FULLY_REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment already FULLY_REFUNDED.");
        } else if (payment.getPaymentStatus() == PaymentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment not SUCCESS yet.");
        }
        payment.setRefundPrice(payment.getTotalPrice());
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
