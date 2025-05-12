package com.analyticalplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private BigDecimal price;
    private long quantity; // Change to primitive long
    private BigDecimal totalAmount;
    private String transactionType; // BUY or SELL
    private LocalDateTime transactionTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}