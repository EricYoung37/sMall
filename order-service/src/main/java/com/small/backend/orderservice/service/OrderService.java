package com.small.backend.orderservice.service;

import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.RefundDto;
import com.small.backend.orderservice.entity.Order;

import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderDto orderDto, String userEmail);
    Order getOrder(UUID orderId);
    Order markOrderAsPaid(UUID orderId);
    Order completeOrder(UUID orderId);
    Order cancelOrder(UUID orderId);
    Order refund(UUID orderId, RefundDto refundDto);
}
