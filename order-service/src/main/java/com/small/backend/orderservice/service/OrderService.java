package com.small.backend.orderservice.service;

import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.OrderRefundDto;
import com.small.backend.orderservice.entity.Order;
import dto.PaymentOrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    PaymentOrderResponse createOrder(OrderDto orderDto, String userEmail);
    List<Order> getOrdersByEmail(String email);
    Order getOrder(String email, UUID orderId);
    Order markOrderAsPaid(PaymentOrderResponse paymentOrderResponse);
    Order completeOrder(String email, UUID orderId);
    Order cancelOrder(String email, UUID orderId);
    Order refund(String email, UUID orderId, OrderRefundDto orderRefundDto);
}
