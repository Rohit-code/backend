package com.analyticalplatform.repository;

import com.analyticalplatform.model.UserStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {
    List<UserStock> findByUserId(Long userId);

    Optional<UserStock> findByUserIdAndSymbol(Long userId, String symbol);
}
