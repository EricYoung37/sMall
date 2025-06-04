package com.small.backend.orderservice.service.impl;

import com.small.backend.orderservice.dao.OrderRepository;
import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.OrderRefundDto;
import com.small.backend.orderservice.entity.Order;
import com.small.backend.orderservice.entity.OrderItem;
import com.small.backend.orderservice.entity.OrderPrimaryKey;
import com.small.backend.orderservice.entity.OrderStatus;
import com.small.backend.orderservice.kafka.KafkaOrderListener;
import com.small.backend.orderservice.kafka.KafkaOrderProducer;
import com.small.backend.orderservice.service.OrderService;
import dto.PaymentOrderResponse;
import dto.OrderPaymentDto;
import dto.PaymentRefundDto;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final KafkaOrderProducer kafkaOrderProducer;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, KafkaOrderProducer kafkaOrderProducer) {
        this.orderRepository = orderRepository;
        this.kafkaOrderProducer = kafkaOrderProducer;
    }

    @Override
    public PaymentOrderResponse createOrder(OrderDto orderDto, String userEmail) {
        Order order = new Order();

        OrderPrimaryKey key = new OrderPrimaryKey();
        key.setUserEmail(userEmail);
        key.setOrderId(UUID.randomUUID()); // collision is extremely rare
        order.setKey(key);

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

        OrderPaymentDto paymentRequest = new OrderPaymentDto();
        paymentRequest.setUserEmail(userEmail);
        paymentRequest.setOrderId(order.getKey().getOrderId());
        paymentRequest.setTotalPrice(order.getTotalPrice());

        CompletableFuture<PaymentOrderResponse> future = new CompletableFuture<>();
        KafkaOrderListener.responseMap.put(order.getKey().getOrderId().toString(), future);

        try {
            kafkaOrderProducer.sendPaymentRequest(paymentRequest);
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to notify payment service", e);
        }

        try {
            PaymentOrderResponse response = future.get(); // blocks until response received
            orderRepository.save(order);
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment service failed or timed out");
        } finally {
            KafkaOrderListener.responseMap.remove(order.getKey().getOrderId().toString());
        }
    }

    @Override
    public Order getOrder(String userEmail, UUID orderId) {
        OrderPrimaryKey key = new OrderPrimaryKey();
        key.setUserEmail(userEmail);
        key.setOrderId(orderId);

        return orderRepository.findById(key).orElseThrow(
                () -> new ResourceNotFoundException("Order not found for user: " + userEmail + " and orderId: " + orderId)
        );
    }

    @Override
    public List<Order> getOrdersByEmail(String email) {
        List<Order> orders = orderRepository.findByKeyUserEmail(email);
        if (orders == null || orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for user: " + email);
        }
        return orders;
    }

    @Override
    public Order markOrderAsPaid(PaymentOrderResponse paymentOrderResponse) {
        Order order = getOrder(paymentOrderResponse.getUserEmail(), paymentOrderResponse.getOrderId());

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not in CREATED status");
        }

        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order completeOrder(String email, UUID orderId) {
        Order order = getOrder(email, orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is not in PAID status");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(String email, UUID orderId) {
        // Assume the order exists as the frontend creates valid requests based on data from the backend.
        Order order = getOrder(email, orderId);

        // Only paid orders that are not completed (not delivered) can be canceled.
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not cancellable, status: " + order.getStatus());
        }

        try {
            PaymentRefundDto cancelRequest = new PaymentRefundDto();
            cancelRequest.setOrderId(orderId);
            cancelRequest.setRefundPrice(order.getTotalPrice());
            kafkaOrderProducer.sendCancelRequest(cancelRequest).get(); // synchronous wait
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to notify payment service", e);
        }

        // quantity == refundQuantity for each item
        order.getItems().forEach(item -> item.setRefundQuantity(item.getQuantity()));
        // refundPrice == totalPrice for the order
        order.setRefundPrice(order.getTotalPrice());
        order.setStatus(OrderStatus.FULLY_REFUNDED);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }

    @Override
    public Order refund(String email, UUID orderId, OrderRefundDto orderRefundDto) {
        // Reasonable assumptions are made as the frontend creates valid requests based on data from the backend.
        Order order = getOrder(email, orderId); // Assumption 1: The order exists.
        if (order.getStatus() == OrderStatus.FULLY_REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order already FULLY_REFUNDED");
        } else if (order.getStatus() != OrderStatus.PARTIALLY_REFUNDED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order has not reached COMPLETED status");
        }

        // 1. Calculate the refund amount (no mutation as roll-back may occur if payment-service fails).
        Map<UUID, Integer> refundItems = orderRefundDto.getItems();
        double refundRequestAmount = order.getItems().stream()
                // Assumption 2: The item id in the refund request exists in the original order.
                .filter(orderItem -> refundItems.containsKey(orderItem.getItemId()))
                .mapToDouble(orderItem -> {
                    int requestedRefundQty = refundItems.get(orderItem.getItemId());
                    int currentRefundQty = orderItem.getRefundQuantity() == null? 0 : orderItem.getRefundQuantity();

                    // Assumption 3: The refund quantity for each item does not exceed the quantity of the item.
                    // This check is not needed if assumption 2 holds.
                    if (currentRefundQty + requestedRefundQty > orderItem.getQuantity()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Refund quantity exceeds the quantity of the item");
                    }

                    return requestedRefundQty * orderItem.getUnitPrice();
                }).sum();

        try {
            PaymentRefundDto refundRequest = new PaymentRefundDto();
            refundRequest.setOrderId(orderId);
            refundRequest.setRefundPrice(refundRequestAmount);
            kafkaOrderProducer.sendRefundRequest(refundRequest).get();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to notify payment service", e);
        }

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
