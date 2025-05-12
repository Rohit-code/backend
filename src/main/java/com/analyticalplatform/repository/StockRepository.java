package com.analyticalplatform.repository;

import com.analyticalplatform.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {  // Changed to String to match Stock entity
    @Query("SELECT s FROM Stock s ORDER BY s.percentChange DESC LIMIT 10")
    List<Stock> findTopPerformers();

    @Query("SELECT s FROM Stock s ORDER BY s.percentChange ASC LIMIT 10")
    List<Stock> findWorstPerformers();

    List<Stock> findBySymbolIn(List<String> symbols);
}