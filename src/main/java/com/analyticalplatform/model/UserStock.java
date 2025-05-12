package com.analyticalplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private long quantity; // Change to primitive long
    private BigDecimal averageBuyPrice;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}