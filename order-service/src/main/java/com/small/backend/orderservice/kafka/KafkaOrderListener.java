package com.small.backend.orderservice.kafka;

import com.small.backend.orderservice.service.OrderService;
import dto.PaymentOrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import util.KafkaTopics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KafkaOrderListener {

    private final OrderService orderService;
    public static final ConcurrentHashMap<String, CompletableFuture<PaymentOrderResponse>> responseMap = new ConcurrentHashMap<>();

    @Autowired
    public KafkaOrderListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAYMENT_RESPONSE, groupId = "order-payment-group", containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentOrderResponse(PaymentOrderResponse response) {
        String key = response.getOrderId().toString();
        if (responseMap.containsKey(key)) {
            responseMap.get(key).complete(response);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAYMENT_SUBMIT, groupId = "order-payment-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleSubmitPayment(PaymentOrderResponse response) {
        orderService.markOrderAsPaid(response);
    }
}
