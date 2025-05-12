package com.analyticalplatform.controller;

import com.analyticalplatform.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.analyticalplatform.service.AnalyticsService;
import com.analyticalplatform.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics API", description = "API for user analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/user-metrics")
    @ApiOperation(value = "Get user metrics", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Map<String, Object>> getUserMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        User user = getCurrentUser();
        Map<String, Object> metrics = analyticsService.calculateUserMetrics(user.getId(), startDate, endDate);

        return ResponseEntity.ok(metrics);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}