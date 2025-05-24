package com.analyticalplatform.repository;

import com.analyticalplatform.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);

    List<PriceAlert> findByUserIdAndSymbol(Long userId, String symbol);

    @Query("SELECT pa FROM PriceAlert pa WHERE pa.triggered = false")
    List<PriceAlert> findAllActiveAlerts();

    @Query("SELECT pa FROM PriceAlert pa WHERE pa.triggered = false AND pa.symbol = :symbol")
    List<PriceAlert> findActiveAlertsBySymbol(String symbol);
}