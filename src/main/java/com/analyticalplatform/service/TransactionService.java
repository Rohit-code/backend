package com.analyticalplatform.service;

import com.analyticalplatform.dto.TransactionDTO;
import com.analyticalplatform.dto.WalletOperationDTO;
import com.analyticalplatform.exception.InsufficientFundsException;
import com.analyticalplatform.model.Stock;
import com.analyticalplatform.model.StockTransaction;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.repository.StockRepository;
import com.analyticalplatform.repository.StockTransactionRepository;
import com.analyticalplatform.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final StockTransactionRepository transactionRepository;
    private final UserStockRepository userStockRepository;
    private final StockRepository stockRepository;
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;
    private final WalletService walletService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;

    @Transactional
    public StockTransaction executeTransaction(User user, TransactionDTO transactionDTO) {
        // Get current stock price
        String symbol = transactionDTO.getSymbol().toUpperCase();
        BigDecimal price;

        // Check if we have the stock in our database
        Optional<Stock> stockOpt = stockRepository.findById(symbol);

        if (stockOpt.isPresent() && stockOpt.get().getLastUpdated() != null
                && stockOpt.get().getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
            // Use cached price if less than 5 minutes old
            price = stockOpt.get().getCurrentPrice();
            log.info("Using cached price for {}: ${}", symbol, price);
        } else {
            // Get fresh price from Alpha Vantage
            try {
                apiRateLimiterService.acquirePermit();
                Map<String, Object> quoteData = alphaVantageService.getGlobalQuote(symbol);

                if (quoteData != null && quoteData.containsKey("Global Quote")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> quote = (Map<String, Object>) quoteData.get("Global Quote");
                    String priceStr = (String) quote.get("05. price");

                    if (priceStr != null) {
                        price = new BigDecimal(priceStr);

                        // Update stock in database
                        Stock stock = stockOpt.orElse(new Stock());
                        stock.setSymbol(symbol);
                        stock.setCurrentPrice(price);

                        // Set other values if available
                        if (quote.containsKey("08. previous close")) {
                            String prevCloseStr = (String) quote.get("08. previous close");
                            BigDecimal prevClose = new BigDecimal(prevCloseStr);
                            stock.setPreviousClose(prevClose);

                            // Calculate percent change
                            if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal change = price.subtract(prevClose);
                                BigDecimal percentChange = change.divide(prevClose, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                                stock.setPercentChange(percentChange);
                            }
                        }

                        if (quote.containsKey("06. volume")) {
                            String volumeStr = (String) quote.get("06. volume");
                            stock.setVolume(Long.parseLong(volumeStr));
                        }

                        // Try to get company name if not already set
                        if (stock.getCompanyName() == null || stock.getCompanyName().isEmpty()) {
                            try {
                                apiRateLimiterService.acquirePermit();
                                Map<String, Object> companyData = alphaVantageService.getCompanyOverview(symbol);

                                if (companyData != null && companyData.containsKey("Name")) {
                                    stock.setCompanyName((String) companyData.get("Name"));
                                } else {
                                    stock.setCompanyName(symbol); // Use symbol as fallback
                                }
                            } catch (Exception e) {
                                log.warn("Could not get company name for {}: {}", symbol, e.getMessage());
                                stock.setCompanyName(symbol); // Use symbol as fallback
                            }
                        }

                        stock.setLastUpdated(LocalDateTime.now());
                        stockRepository.save(stock);
                        log.info("Updated stock price for {}: ${}", symbol, price);
                    } else {
                        throw new RuntimeException("Could not get current price for stock: " + symbol);
                    }
                } else {
                    throw new RuntimeException("Invalid quote data for stock: " + symbol);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("API rate limit reached. Please try again later.", e);
            } catch (Exception e) {
                throw new RuntimeException("Error getting stock price: " + e.getMessage(), e);
            }
        }

        // Calculate transaction amount
        long quantity = transactionDTO.getQuantity();
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));

        // Process transaction based on type (BUY/SELL)
        if ("BUY".equals(transactionDTO.getTransactionType())) {
            // Check if user has enough funds
            if (!walletService.hasSufficientFunds(user, totalAmount)) {
                throw new InsufficientFundsException("Insufficient funds to complete purchase. Required: $" + totalAmount);
            }

            // Withdraw funds from wallet
            walletService.withdraw(user, new WalletOperationDTO(totalAmount, "Purchase of " + quantity + " " + symbol + " shares"));
        } else if ("SELL".equals(transactionDTO.getTransactionType())) {
            // Check if user has enough stocks to sell (handled in updateUserPortfolio)
            // If successful, deposit funds to wallet
            walletService.deposit(user, new WalletOperationDTO(totalAmount, "Sale of " + quantity + " " + symbol + " shares"));
        } else {
            throw new IllegalArgumentException("Invalid transaction type: " + transactionDTO.getTransactionType());
        }

        // Create transaction record
        StockTransaction transaction = StockTransaction.builder()
                .symbol(symbol)
                .price(price)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .transactionType(transactionDTO.getTransactionType())
                .transactionTime(LocalDateTime.now())
                .user(user)
                .build();

        // Update user's stock portfolio
        updateUserPortfolio(user, transaction);

        // Save transaction
        transaction = transactionRepository.save(transaction);

        // Send notification
        notificationService.sendTransactionNotification(
                user,
                transaction.getSymbol(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getTransactionType()
        );

        // Audit the transaction
        auditService.logAction(user.getUsername(), transactionDTO.getTransactionType(),
                "StockTransaction", transaction.getId().toString(),
                transactionDTO.getTransactionType() + " " + quantity + " shares of " + symbol + " at $" + price);

        return transaction;
    }

    private void updateUserPortfolio(User user, StockTransaction transaction) {
        String symbol = transaction.getSymbol();
        long quantity = transaction.getQuantity();
        String transactionType = transaction.getTransactionType();

        Optional<UserStock> userStockOpt = userStockRepository.findByUserIdAndSymbol(user.getId(), symbol);

        if ("BUY".equals(transactionType)) {
            if (userStockOpt.isPresent()) {
                UserStock userStock = userStockOpt.get();

                // Update average buy price
                long oldQuantity = userStock.getQuantity();
                BigDecimal oldTotalValue = userStock.getAverageBuyPrice().multiply(BigDecimal.valueOf(oldQuantity));
                BigDecimal newValue = transaction.getPrice().multiply(BigDecimal.valueOf(quantity));
                long newQuantity = oldQuantity + quantity;

                BigDecimal newAveragePrice = oldTotalValue.add(newValue)
                        .divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);

                userStock.setQuantity(newQuantity);
                userStock.setAverageBuyPrice(newAveragePrice);

                userStockRepository.save(userStock);
            } else {
                // Create new user stock entry
                UserStock userStock = UserStock.builder()
                        .user(user)
                        .symbol(symbol)
                        .quantity(quantity)
                        .averageBuyPrice(transaction.getPrice())
                        .build();

                userStockRepository.save(userStock);
            }
        } else if ("SELL".equals(transactionType)) {
            if (userStockOpt.isPresent()) {
                UserStock userStock = userStockOpt.get();

                // Check if user has enough stocks to sell
                if (userStock.getQuantity() < quantity) {
                    throw new RuntimeException("Not enough stocks to sell: you have " + userStock.getQuantity() +
                            " shares of " + symbol);
                }

                // Update quantity
                long newQuantity = userStock.getQuantity() - quantity;

                if (newQuantity > 0) {
                    userStock.setQuantity(newQuantity);
                    userStockRepository.save(userStock);
                } else {
                    // If quantity becomes 0, remove the entry
                    userStockRepository.delete(userStock);
                }
            } else {
                throw new RuntimeException("No stocks available to sell: you don't own any shares of " + symbol);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<StockTransaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionTimeDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UserStock> getUserPortfolio(Long userId) {
        return userStockRepository.findByUserId(userId);
    }

    /**
     * Calculate the total value of a user's portfolio using current stock prices
     */
    @Transactional(readOnly = true)
    public BigDecimal calculatePortfolioValue(Long userId) {
        List<UserStock> portfolio = userStockRepository.findByUserId(userId);
        BigDecimal totalValue = BigDecimal.ZERO;

        for (UserStock holding : portfolio) {
            BigDecimal currentPrice = null;

            // Try to get current price from stock repository
            Optional<Stock> stockOpt = stockRepository.findById(holding.getSymbol());
            if (stockOpt.isPresent() && stockOpt.get().getCurrentPrice() != null) {
                currentPrice = stockOpt.get().getCurrentPrice();
            } else {
                // Fallback to average buy price
                currentPrice = holding.getAverageBuyPrice();
            }

            BigDecimal holdingValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
            totalValue = totalValue.add(holdingValue);
        }

        return totalValue;
    }

    /**
     * Get total profit/loss for a user based on current prices vs. purchase prices
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculatePortfolioProfitLoss(Long userId) {
        List<UserStock> portfolio = userStockRepository.findByUserId(userId);
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;

        for (UserStock holding : portfolio) {
            BigDecimal buyValue = holding.getAverageBuyPrice()
                    .multiply(BigDecimal.valueOf(holding.getQuantity()));
            totalCost = totalCost.add(buyValue);

            BigDecimal currentPrice = null;

            // Try to get current price from stock repository
            Optional<Stock> stockOpt = stockRepository.findById(holding.getSymbol());
            if (stockOpt.isPresent() && stockOpt.get().getCurrentPrice() != null) {
                currentPrice = stockOpt.get().getCurrentPrice();
            } else {
                // Fallback to average buy price
                currentPrice = holding.getAverageBuyPrice();
            }

            BigDecimal holdingValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));
            currentValue = currentValue.add(holdingValue);
        }

        BigDecimal profitLoss = currentValue.subtract(totalCost);
        BigDecimal profitLossPercentage = BigDecimal.ZERO;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercentage = profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return Map.of(
                "totalCost", totalCost,
                "currentValue", currentValue,
                "profitLoss", profitLoss,
                "profitLossPercentage", profitLossPercentage
        );
    }
}