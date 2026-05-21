package com.rishita.fraudservice.controller;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping(path = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        ServletWebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> attrs = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
        int status = (int) attrs.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(status).body(attrs);
    }
}
