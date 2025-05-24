package com.analyticalplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlertDTO {
    private Long id;

    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    @NotNull(message = "Target price is required")
    @DecimalMin(value = "0.01", message = "Target price must be greater than zero")
    private BigDecimal targetPrice;

    @NotBlank(message = "Alert type is required")
    private String alertType; // "ABOVE" or "BELOW"

    private boolean triggered;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}