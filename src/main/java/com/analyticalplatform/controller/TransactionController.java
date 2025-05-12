package com.analyticalplatform.controller;

import com.analyticalplatform.dto.TransactionDTO;
import com.analyticalplatform.model.StockTransaction;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.service.TransactionService;
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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction API", description = "API for stock transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;

    @PostMapping
    @ApiOperation(value = "Execute a stock transaction", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> executeTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        try {
            User user = getCurrentUser();
            StockTransaction transaction = transactionService.executeTransaction(user, transactionDTO);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @ApiOperation(value = "Get user's transaction history", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<StockTransaction>> getUserTransactions() {
        User user = getCurrentUser();
        return ResponseEntity.ok(transactionService.getUserTransactions(user.getId()));
    }

    @GetMapping("/portfolio")
    @ApiOperation(value = "Get user's stock portfolio", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<UserStock>> getUserPortfolio() {
        User user = getCurrentUser();
        return ResponseEntity.ok(transactionService.getUserPortfolio(user.getId()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}