package com.analyticalplatform.service;

import com.analyticalplatform.dto.WalletDTO;
import com.analyticalplatform.dto.WalletOperationDTO;
import com.analyticalplatform.exception.InsufficientFundsException;
import com.analyticalplatform.exception.ResourceNotFoundException;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.Wallet;
import com.analyticalplatform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final AuditService auditService;

    @Transactional
    public WalletDTO getWallet(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getUsername()));

        return mapToDTO(wallet);
    }

    @Transactional
    public WalletDTO createWallet(User user, BigDecimal initialBalance) {
        // Check if wallet already exists
        walletRepository.findByUserId(user.getId()).ifPresent(w -> {
            throw new RuntimeException("Wallet already exists for user: " + user.getUsername());
        });

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(initialBalance)
                .version(0L)
                .build();

        wallet = walletRepository.save(wallet);

        auditService.logAction(user.getUsername(), "CREATE", "Wallet", wallet.getId().toString(),
                "Created wallet with initial balance: " + initialBalance);

        return mapToDTO(wallet);
    }

    @Transactional
    public WalletDTO deposit(User user, WalletOperationDTO depositDTO) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getUsername()));

        wallet.deposit(depositDTO.getAmount());
        wallet = walletRepository.save(wallet);

        auditService.logAction(user.getUsername(), "DEPOSIT", "Wallet", wallet.getId().toString(),
                "Deposited " + depositDTO.getAmount() + " - " + depositDTO.getDescription());

        return mapToDTO(wallet);
    }

    @Transactional
    public WalletDTO withdraw(User user, WalletOperationDTO withdrawDTO) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getUsername()));

        try {
            wallet.withdraw(withdrawDTO.getAmount());
            wallet = walletRepository.save(wallet);

            auditService.logAction(user.getUsername(), "WITHDRAW", "Wallet", wallet.getId().toString(),
                    "Withdrew " + withdrawDTO.getAmount() + " - " + withdrawDTO.getDescription());

            return mapToDTO(wallet);
        } catch (InsufficientFundsException e) {
            auditService.logAction(user.getUsername(), "WITHDRAW_FAILED", "Wallet", wallet.getId().toString(),
                    "Failed to withdraw " + withdrawDTO.getAmount() + " - Insufficient funds");
            throw e;
        }
    }

    @Transactional
    public boolean hasSufficientFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getUsername()));

        return wallet.hasSufficientFunds(amount);
    }

    private WalletDTO mapToDTO(Wallet wallet) {
        return WalletDTO.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .build();
    }
}