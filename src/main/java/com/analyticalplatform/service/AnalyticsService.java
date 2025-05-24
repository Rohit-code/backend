package com.analyticalplatform.service;

import com.analyticalplatform.model.StockTransaction;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final StockTransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;

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

        // Current portfolio value with updated prices
        List<UserStock> portfolio = transactionService.getUserPortfolio(userId);
        BigDecimal portfolioValue = calculatePortfolioValue(portfolio);

        metrics.put("portfolioValue", portfolioValue);

        // Add market indicators if possible
        try {
            addMarketIndicators(metrics);
        } catch (Exception e) {
            log.warn("Could not add market indicators to analytics: {}", e.getMessage());
        }

        return metrics;
    }

    private BigDecimal calculatePortfolioValue(List<UserStock> portfolio) {
        BigDecimal totalValue = BigDecimal.ZERO;

        for (UserStock holding : portfolio) {
            try {
                // Try to get current price
                apiRateLimiterService.acquirePermit();
                Map<String, Object> quoteData = alphaVantageService.getGlobalQuote(holding.getSymbol());

                if (quoteData != null && quoteData.containsKey("Global Quote")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> quote = (Map<String, Object>) quoteData.get("Global Quote");
                    String priceStr = (String) quote.get("05. price");

                    if (priceStr != null) {
                        BigDecimal currentPrice = new BigDecimal(priceStr);
                        BigDecimal holdingValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
                        totalValue = totalValue.add(holdingValue);
                    } else {
                        // Fall back to average buy price
                        BigDecimal holdingValue = holding.getAverageBuyPrice()
                                .multiply(BigDecimal.valueOf(holding.getQuantity()));
                        totalValue = totalValue.add(holdingValue);
                    }
                } else {
                    // Fall back to average buy price
                    BigDecimal holdingValue = holding.getAverageBuyPrice()
                            .multiply(BigDecimal.valueOf(holding.getQuantity()));
                    totalValue = totalValue.add(holdingValue);
                }
            } catch (InterruptedException e) {
                log.warn("API rate limit reached when calculating portfolio value, using average buy price");
                // Fall back to average buy price
                BigDecimal holdingValue = holding.getAverageBuyPrice()
                        .multiply(BigDecimal.valueOf(holding.getQuantity()));
                totalValue = totalValue.add(holdingValue);
            } catch (Exception e) {
                log.error("Error getting current price for {}: {}", holding.getSymbol(), e.getMessage());
                // Fall back to average buy price
                BigDecimal holdingValue = holding.getAverageBuyPrice()
                        .multiply(BigDecimal.valueOf(holding.getQuantity()));
                totalValue = totalValue.add(holdingValue);
            }
        }

        return totalValue;
    }

    private void addMarketIndicators(Map<String, Object> metrics) {
        try {
            // Get market status
            apiRateLimiterService.acquirePermit();
            Map<String, Object> topStocks = alphaVantageService.getTopGainersLosers();

            if (topStocks != null) {
                // Add top gainers and losers
                metrics.put("marketIndicators", topStocks);
            }
        } catch (InterruptedException e) {
            log.warn("API rate limit reached when getting market indicators");
        } catch (Exception e) {
            log.error("Error getting market indicators: {}", e.getMessage());
        }
    }
}