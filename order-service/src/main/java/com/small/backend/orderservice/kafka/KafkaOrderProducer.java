package com.small.backend.orderservice.kafka;

import dto.OrderPaymentDto;
import dto.PaymentRefundDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import util.KafkaTopics;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaOrderProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> sendPaymentRequest(OrderPaymentDto dto) {
        return kafkaTemplate.send(KafkaTopics.ORDER_PAYMENT_REQUEST, dto);
    }

    public CompletableFuture<SendResult<String, Object>> sendCancelRequest(PaymentRefundDto dto) {
        return kafkaTemplate.send(KafkaTopics.ORDER_CANCEL_REQUEST, dto);
    }

    public CompletableFuture<SendResult<String, Object>> sendRefundRequest(PaymentRefundDto dto) {
        return kafkaTemplate.send(KafkaTopics.ORDER_REFUND_REQUEST, dto);
    }
}
