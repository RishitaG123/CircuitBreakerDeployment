package com.rishita.paymentservice.service;

import com.rishita.paymentservice.entity.Payment;
import com.rishita.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository repo;

    public PaymentService(PaymentRepository repo) {
        this.repo = repo;
    }

    public Payment save(Payment p) {
        return repo.save(p);
    }

    public List<Payment> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }


}
