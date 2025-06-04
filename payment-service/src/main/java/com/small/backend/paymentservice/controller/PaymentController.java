package com.small.backend.paymentservice.controller;

import com.small.backend.paymentservice.dto.UserPaymentDto;
import com.small.backend.paymentservice.entity.Payment;
import com.small.backend.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // This is the redirect URL returned from the payment service to the order service upon payment creation.
    // The frontend will display a pay button based on the payment status (CREATED).
    @GetMapping("{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    // Allows the user to view the payment information when viewing an order.
    @GetMapping
    public ResponseEntity<Payment> getPaymentByOrderId(@RequestParam("orderId") UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("{id}/submit")
    public ResponseEntity<Payment> submitPayment(@PathVariable("id") UUID id,
                                                 @RequestBody @Valid UserPaymentDto userPaymentDto) {
        return ResponseEntity.ok(paymentService.submitPayment(id, userPaymentDto.getPaymentMethod()));
    }
}
