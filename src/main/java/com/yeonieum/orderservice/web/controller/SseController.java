package com.yeonieum.orderservice.web.controller;

import com.yeonieum.orderservice.domain.notification.service.OrderNotificationServiceForCustomer;
import com.yeonieum.orderservice.global.auth.Role;
import com.yeonieum.orderservice.global.usercontext.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-notification")
public class SseController {
    private final OrderNotificationServiceForCustomer notificationService;

    @Role(role = {"ROLE_CUSTOMER"}, url = "/api/order-notification/{customerId}/subscription", method = "GET")
    @GetMapping(value = "/{customerId}/subscription", produces = MediaType.ALL_VALUE)
    public ResponseEntity<SseEmitter> connect(@PathVariable Long customerId) throws IOException {
        Long customer = Long.valueOf(UserContextHolder.getContext().getUniqueId());
        return ResponseEntity.ok(notificationService.subscribe(customer));
    }
}
