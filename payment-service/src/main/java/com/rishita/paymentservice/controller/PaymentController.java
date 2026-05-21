package com.rishita.paymentservice.controller;

import com.rishita.paymentservice.dto.PaymentRequest;
import com.rishita.paymentservice.entity.Payment;
import com.rishita.paymentservice.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/pay")
    public ResponseEntity<String> pay(@RequestBody PaymentRequest request) {
        Payment p = new Payment(request.getUserId(), request.getAmount());
        service.save(p);
        return ResponseEntity.ok("Payment processed for user " + request.getUserId() + ", amount " + request.getAmount());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getByUser(@PathVariable Long userId) {
        List<Payment> list = service.findByUserId(userId);
        return ResponseEntity.ok(list);
    }
}
