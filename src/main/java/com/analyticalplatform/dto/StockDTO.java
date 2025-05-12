package com.analyticalplatform.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockDTO {
    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal percentChange;
    private Long volume;
    private LocalDateTime lastUpdated;
}
