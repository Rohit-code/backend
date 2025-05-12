package com.analyticalplatform.service;

import com.analyticalplatform.model.StockTransaction;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final StockTransactionRepository transactionRepository;
    private final TransactionService transactionService;

    /**
     * Calculate transaction metrics for a user in a date range
     */
    public Map<String, Object> calculateUserMetrics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<StockTransaction> transactions = transactionRepository.findUserTransactionsInDateRange(
                userId, startDate, endDate);

        Map<String, Object> metrics = new HashMap<>();

        // Total transactions
        metrics.put("totalTransactions", transactions.size());

        // Buy vs Sell transactions
        long buyTransactions = transactions.stream()
                .filter(t -> "BUY".equals(t.getTransactionType()))
                .count();

        long sellTransactions = transactions.stream()
                .filter(t -> "SELL".equals(t.getTransactionType()))
                .count();

        metrics.put("buyTransactions", buyTransactions);
        metrics.put("sellTransactions", sellTransactions);

        // Total amount spent/received
        BigDecimal totalBuyAmount = transactions.stream()
                .filter(t -> "BUY".equals(t.getTransactionType()))
                .map(StockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = transactions.stream()
                .filter(t -> "SELL".equals(t.getTransactionType()))
                .map(StockTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        metrics.put("totalBuyAmount", totalBuyAmount);
        metrics.put("totalSellAmount", totalSellAmount);

        // Most traded stocks
        Map<String, Long> stockCounts = transactions.stream()
                .collect(Collectors.groupingBy(StockTransaction::getSymbol, Collectors.counting()));

        metrics.put("mostTradedStocks", stockCounts);

        // Current portfolio value
        List<UserStock> portfolio = transactionService.getUserPortfolio(userId);
        BigDecimal portfolioValue = BigDecimal.ZERO;

        // In a real application, you would get the current price for each stock
        // and calculate the actual portfolio value

        metrics.put("portfolioValue", portfolioValue);

        return metrics;
    }
}