package com.analyticalplatform.service;

import com.analyticalplatform.dto.UserRegistrationDTO;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.Wallet;
import com.analyticalplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${user.initial.balance:1000.00}")
    private BigDecimal initialBalance;

    @Transactional
    public User registerUser(UserRegistrationDTO registrationDTO) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(registrationDTO.getUsername())
                .email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .roles(Collections.singleton("ROLE_USER"))
                .build();

        // Create a wallet with initial balance
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(initialBalance)
                .version(0L)
                .build();

        user.setWallet(wallet);

        user = userRepository.save(user);

        // Log the registration
        auditService.logAction(user.getUsername(), "REGISTER", "User", user.getId().toString(),
                "New user registered");

        return user;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}