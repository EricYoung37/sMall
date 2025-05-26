package com.small.backend.orderservice.service.impl;

import com.small.backend.orderservice.dao.OrderRepository;
import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.RefundDto;
import com.small.backend.orderservice.entity.Order;
import com.small.backend.orderservice.entity.OrderItem;
import com.small.backend.orderservice.entity.OrderStatus;
import com.small.backend.orderservice.service.OrderService;
import exception.ResourceNotFoundException;
import jakarta.ws.rs.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order createOrder(OrderDto orderDto, String userEmail) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        order.setUserEmail(userEmail);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setShippingAddress(orderDto.getShippingAddress());

        List<OrderItem> orderItems = orderDto.getItems().stream()
                .map(itemDto -> {
                    OrderItem item = new OrderItem();
                    item.setItemId(itemDto.getItemId());
                    item.setItemName(itemDto.getItemName());
                    item.setQuantity(itemDto.getQuantity());
                    item.setRefundQuantity(0);
                    item.setUnitPrice(itemDto.getUnitPrice());
                    item.setMerchantId(itemDto.getMerchantId());
                    return item;
                })
                .collect(Collectors.toList());

        order.setTotalPrice(calculateTotalPrice(orderItems));
        order.setRefundPrice(calculateRefundPrice(orderItems));
        order.setItems(orderItems);

        // TODO: Call payment-service to create a PENDING payment.
        // Roll-back if payment-service fails.

        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order with id " + orderId + " not found")
        );
    }

    @Override
    public Order markOrderAsPaid(UUID orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Order is not in CREATED status");
        }
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order completeOrder(UUID orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException("Order is not in PAID status");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(UUID orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.CREATED || order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException("Order is not in CREATED or PAID status (you may request for a refund)");
        }

        // TODO: Call payment-service to update the payment to FULLY_REFUNDED, throws if fails.

        // quantity == refundQuantity for each item
        order.getItems().forEach(item -> item.setRefundQuantity(item.getQuantity()));
        // refundPrice == totalPrice for the order
        order.setRefundPrice(order.getTotalPrice());
        order.setStatus(OrderStatus.FULLY_REFUNDED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order refund(UUID orderId, RefundDto refundDto) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Order is not in COMPLETED status (you may cancel it)");
        } else if (order.getStatus() == OrderStatus.FULLY_REFUNDED) {
            throw new BadRequestException("Order is already fully refunded");
        }

        // 1. Calculate the refund amount (no mutation as roll-back may occur if payment-service fails).
        Map<UUID, Integer> refundItems = refundDto.getItems();
        // Reasonable assumptions are made as the frontend can create valid requests based on data from the backend.
        double refundRequestAmount = order.getItems().stream()
                // Assumption 1: The item id in the refund request exists in the original order.
                .filter(orderItem -> refundItems.containsKey(orderItem.getItemId()))
                .mapToDouble(orderItem -> {
                    int requestedRefundQty = refundItems.get(orderItem.getItemId());
                    int currentRefundQty = orderItem.getRefundQuantity() == null? 0 : orderItem.getRefundQuantity();

                    // Assumption 2: The refund quantity for each item does not exceed the quantity of the item.
                    // This check is not needed if assumption 2 holds.
                    if (currentRefundQty + requestedRefundQty > orderItem.getQuantity()) {
                        throw new BadRequestException("Refund quantity exceeds the quantity of the item");
                    }

                    return requestedRefundQty * orderItem.getUnitPrice();
                }).sum();

        // TODO: 2. Call payment-service to update the payment to PARTIALLY_REFUNDED or FULLY_REFUNDED, throws if fails.

        // 3. Update the order items (now safe to mutate).
        // Making a deep copy for the order and do the calculation and mutation in one traversal has worse performance.
        order.getItems().forEach(orderItem -> {
            if (refundItems.containsKey(orderItem.getItemId())) {
                int requestedRefundQty = refundItems.get(orderItem.getItemId());
                int currentRefundQty = orderItem.getRefundQuantity() == null? 0 : orderItem.getRefundQuantity();
                orderItem.setRefundQuantity(currentRefundQty + requestedRefundQty);
            }
        });

        // 4. Update the order refund price.
        double currentRefundPrice = order.getRefundPrice() == null? 0 : order.getRefundPrice();
        double newRefundPrice = currentRefundPrice + refundRequestAmount;
        order.setRefundPrice(newRefundPrice);
        order.setStatus(newRefundPrice < order.getTotalPrice() ?
                OrderStatus.PARTIALLY_REFUNDED : OrderStatus.FULLY_REFUNDED); // Assumption 2 again.
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    private static double calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }

    private static double calculateRefundPrice(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getRefundQuantity())
                .sum();
    }
}
