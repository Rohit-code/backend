package com.analyticalplatform.controller;

import com.analyticalplatform.dto.PriceAlertDTO;
import com.analyticalplatform.model.User;
import com.analyticalplatform.service.PriceAlertService;
import com.analyticalplatform.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Price Alert API", description = "API for price alerts")
public class PriceAlertController {
    private final PriceAlertService priceAlertService;
    private final UserService userService;

    @GetMapping
    @ApiOperation(value = "Get all user alerts", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<PriceAlertDTO>> getAllAlerts() {
        User user = getCurrentUser();
        List<PriceAlertDTO> alerts = priceAlertService.getUserAlerts(user);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/symbol/{symbol}")
    @ApiOperation(value = "Get alerts for a symbol", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<PriceAlertDTO>> getAlertsBySymbol(@PathVariable String symbol) {
        User user = getCurrentUser();
        List<PriceAlertDTO> alerts = priceAlertService.getUserAlertsForSymbol(user, symbol.toUpperCase());
        return ResponseEntity.ok(alerts);
    }

    @PostMapping
    @ApiOperation(value = "Create a new price alert", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> createAlert(@Valid @RequestBody PriceAlertDTO alertDTO) {
        try {
            User user = getCurrentUser();
            PriceAlertDTO createdAlert = priceAlertService.createAlert(user, alertDTO);
            return ResponseEntity.ok(createdAlert);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a price alert", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> deleteAlert(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            priceAlertService.deleteAlert(user, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}