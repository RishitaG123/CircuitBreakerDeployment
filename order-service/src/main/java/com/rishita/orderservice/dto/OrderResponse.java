package com.rishita.orderservice.dto;

public class OrderResponse {
    private String status;
    private String message;

    public OrderResponse() {}
    public OrderResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
