package com.small.backend.paymentservice.kafka;

import dto.PaymentOrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import util.KafkaTopics;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaPaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaPaymentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> sendPaymentSubmit(PaymentOrderResponse response) {
        return kafkaTemplate.send(KafkaTopics.ORDER_PAYMENT_SUBMIT, response);
    }
}
