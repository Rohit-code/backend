package com.analyticalplatform.controller;

import com.analyticalplatform.dto.WalletDTO;
import com.analyticalplatform.dto.WalletOperationDTO;
import com.analyticalplatform.exception.InsufficientFundsException;
import com.analyticalplatform.model.User;
import com.analyticalplatform.service.UserService;
import com.analyticalplatform.service.WalletService;
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

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet API", description = "API for wallet operations")
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;

    @GetMapping
    @ApiOperation(value = "Get user wallet", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<WalletDTO> getWallet() {
        User user = getCurrentUser();
        WalletDTO wallet = walletService.getWallet(user);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/deposit")
    @ApiOperation(value = "Deposit funds", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> deposit(@Valid @RequestBody WalletOperationDTO depositDTO) {
        try {
            User user = getCurrentUser();
            WalletDTO updatedWallet = walletService.deposit(user, depositDTO);
            return ResponseEntity.ok(updatedWallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    @ApiOperation(value = "Withdraw funds", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> withdraw(@Valid @RequestBody WalletOperationDTO withdrawDTO) {
        try {
            User user = getCurrentUser();
            WalletDTO updatedWallet = walletService.withdraw(user, withdrawDTO);
            return ResponseEntity.ok(updatedWallet);
        } catch (InsufficientFundsException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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