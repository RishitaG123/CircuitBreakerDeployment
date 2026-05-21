package com.rishita.orderservice.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishita.orderservice.dto.OrderRequest;
import com.rishita.orderservice.dto.OrderResponse;
import com.rishita.paymentservice.repository.PaymentRepository;
import com.rishita.paymentservice.service.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/orders")
public class OrderController {


//    private static final ObjectMapper MAPPER = new ObjectMapper();
//    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;
    private final String fraudServiceUrl;
    private final String paymentServiceUrl;

    public OrderController(RestTemplate restTemplate,
                           @Value("${services.fraud}") String fraudServiceUrl,
                           @Value("${services.payment}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.fraudServiceUrl = fraudServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        String fraudUrl = fraudServiceUrl + "/api/fraud/check/" + request.getUserId();

        // 1) Call fraud service and handle fraud-down or not-authorized
        // Use a local RestTemplate instance with error handling disabled so we can inspect status codes
        RestTemplate local = new RestTemplate();
        local.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false; // never treat any response as error
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // no-op
            }
        });

        ResponseEntity<String> rawResp;
        try {
            rawResp = local.exchange(fraudUrl, HttpMethod.GET, null, String.class);
        } catch (RestClientException e) {
            // network/connectivity issues
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new OrderResponse("FAILED", "Fraud service is down, payment can not be processed"));
        }

        int fraudStatus = rawResp.getStatusCode().value();
        String rawBody = rawResp.getBody();


        // map codes: 403 => not authorized; non-2xx => service down
        if (fraudStatus == HttpStatus.FORBIDDEN.value()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new OrderResponse("FAILED", "User not authorized"));
        }

        if (fraudStatus < 200 || fraudStatus >= 300) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new OrderResponse("FAILED", "Fraud service is down, payment can not be processed"));
        }

        // 2) Fraud OK -> call payment service
        String payUrl = paymentServiceUrl + "/api/payment/pay";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> paymentResp = restTemplate.postForEntity(payUrl, entity, String.class);

            if (paymentResp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(new OrderResponse("SUCCESS", "User is verified, and payment is done"));
            } else {
                // Treat non-2xx as payment failure/unavailable per your simplified requirements
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new OrderResponse("FAILED", "Payment service is down, payment can not be processed"));
            }
        } catch (RestClientException e) {
            // Payment service unreachable
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new OrderResponse("FAILED", "Payment service is down, payment can not be processed"));
        }
    }


    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = "fraudService", fallbackMethod = "getOrdersFallback")
    public ResponseEntity<Object> getOrdersByUser(@PathVariable("userId") Long userId) {

        String fraudUrl = fraudServiceUrl + "/api/fraud/check/" + userId;

        ResponseEntity<List<Object>> resp = restTemplate.exchange(
                fraudUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Object>>() {}
        );

        return ResponseEntity.ok(resp.getBody());
    }

    public ResponseEntity<Object> getOrdersFallback(Long userId, Exception ex) {

        System.out.println("Fallback reason: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fraud service is unavailable. Please try later.");
    }

}




