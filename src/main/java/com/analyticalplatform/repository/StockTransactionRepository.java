package com.analyticalplatform.repository;

import com.analyticalplatform.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findByUserIdOrderByTransactionTimeDesc(Long userId);

    List<StockTransaction> findByUserIdAndSymbol(Long userId, String symbol);

    @Query("SELECT t FROM StockTransaction t WHERE t.user.id = :userId AND t.transactionTime BETWEEN :startDate AND :endDate")
    List<StockTransaction> findUserTransactionsInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
