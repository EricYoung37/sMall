package com.small.backend.paymentservice.kafka;

import com.small.backend.paymentservice.service.PaymentService;
import dto.OrderPaymentDto;
import dto.PaymentOrderResponse;
import dto.PaymentRefundDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import util.KafkaTopics;

@Service
public class KafkaPaymentListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaPaymentListener(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAYMENT_REQUEST, groupId = "order-payment-group", containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentCreate(OrderPaymentDto dto) {
        PaymentOrderResponse response = paymentService.createPayment(dto);
        kafkaTemplate.send(KafkaTopics.ORDER_PAYMENT_RESPONSE, response);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCEL_REQUEST, groupId = "order-payment-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleCancelRequest(PaymentRefundDto dto) {
        paymentService.cancelByOrderId(dto.getOrderId());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_REFUND_REQUEST, groupId = "order-payment-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleRefundRequest(PaymentRefundDto dto) {
        paymentService.refundByOrderId(dto.getOrderId(), dto.getRefundPrice());
    }
}
