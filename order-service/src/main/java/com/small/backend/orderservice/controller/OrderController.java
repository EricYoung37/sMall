package com.small.backend.orderservice.controller;

import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.OrderRefundDto;
import com.small.backend.orderservice.entity.Order;
import com.small.backend.orderservice.service.OrderService;
import dto.PaymentOrderResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<PaymentOrderResponse> createOrder(@RequestBody @Valid OrderDto orderDto,
                                                            Authentication auth){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(orderDto, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getOrdersByEmail(auth.getName()));
    }

    @GetMapping("{id}")
    public ResponseEntity<Order> getOrder(@PathVariable("id") UUID id, Authentication auth) {
        return ResponseEntity.ok(orderService.getOrder(auth.getName(), id));
    }

    // Suppose a delivery service call this endpoint with an internal auth token (see SecurityConfig).
    @PostMapping("{id}/complete")
    public ResponseEntity<Order> completeOrder(@PathVariable("id") UUID id,
                                               @RequestParam("userEmail") String email) {
        return ResponseEntity.ok(orderService.completeOrder(email, id));
    }

    @PostMapping("{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable("id") UUID id, Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(auth.getName(), id));
    }

    @PostMapping("{id}/refund")
    public ResponseEntity<Order> refund(@PathVariable("id") UUID id,
                                        Authentication auth,
                                        @RequestBody @Valid OrderRefundDto orderRefundDto) {
        return ResponseEntity.ok(orderService.refund(auth.getName(), id, orderRefundDto));
    }
}
