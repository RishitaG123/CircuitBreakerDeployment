package com.rishita.paymentservice.dto;

public class PaymentRequest {
    private Long userId;
    private Double amount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
