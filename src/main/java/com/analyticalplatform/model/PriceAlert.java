package com.analyticalplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private BigDecimal targetPrice;
    private String alertType; // "ABOVE" or "BELOW"
    private boolean triggered;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Business logic methods
    public boolean shouldTrigger(BigDecimal currentPrice) {
        if (triggered) {
            return false;
        }

        if ("ABOVE".equals(alertType)) {
            return currentPrice.compareTo(targetPrice) >= 0;
        } else if ("BELOW".equals(alertType)) {
            return currentPrice.compareTo(targetPrice) <= 0;
        }

        return false;
    }

    public void markAsTriggered() {
        this.triggered = true;
        this.triggeredAt = LocalDateTime.now();
    }
}