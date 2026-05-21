package com.rishita.fraudservice.controller;

import com.rishita.fraudservice.repository.AuthorizedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private static final Logger log = LoggerFactory.getLogger(FraudController.class);

    @Autowired(required = false)
    private AuthorizedUserRepository repo;

    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Object>> checkUser(@PathVariable("userId") Long userId) {
        if (repo == null) {
            log.warn("AuthorizedUserRepository bean not available (DB down?) - returning 503");
            Map<String, Object> m = new HashMap<>();
            m.put("authorized", false);
            m.put("message", "Fraud service not available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }

        try {
            boolean authorized = repo.findByUserId(userId).isPresent();
            if (authorized) {
                Map<String, Object> m = new HashMap<>();
                m.put("authorized", true);
                m.put("message", "User authorized");
                return ResponseEntity.ok(m);
            } else {
                Map<String, Object> m = new HashMap<>();
                m.put("authorized", false);
                m.put("message", "User not authorized");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(m);
            }
        } catch (Exception ex) {
            // If the DB is down or any unexpected error occurs, return 503 with a clear message
            log.error("Error checking user {}: {}", userId, ex.toString());
            Map<String, Object> m = new HashMap<>();
            m.put("authorized", false);
            m.put("message", "Fraud service error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
        }
    }

}
