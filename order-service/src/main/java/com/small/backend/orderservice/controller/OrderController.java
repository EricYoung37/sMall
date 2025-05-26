package com.small.backend.orderservice.controller;

import com.small.backend.orderservice.dto.OrderDto;
import com.small.backend.orderservice.dto.RefundDto;
import com.small.backend.orderservice.entity.Order;
import com.small.backend.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Order> createOrder(@RequestBody @Valid OrderDto orderDto,
                                             Authentication auth){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(orderDto, auth.getName()));
    }

    @GetMapping("{id}")
    public ResponseEntity<Order> getOrder(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // TODO: Called by the service layer of payment-service upon payment SUCCESS.
    @PostMapping("{id}/paid")
    public ResponseEntity<Order> markOrderAsPaid(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.markOrderAsPaid(id));
    }

    // Suppose a delivery service call this endpoint with an internal auth token (see SecurityConfig).
    @PostMapping("{id}/complete")
    public ResponseEntity<Order> completeOrder(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    @PostMapping("{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping("{id}/refund")
    public ResponseEntity<Order> refund(@PathVariable("id") UUID id, RefundDto refundDto) {
        return ResponseEntity.ok(orderService.refund(id, refundDto));
    }
}
