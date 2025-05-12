package com.analyticalplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    private String symbol;  // This is correct - using String as ID
    private String companyName;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal percentChange;
    private Long volume;
    private LocalDateTime lastUpdated;
}