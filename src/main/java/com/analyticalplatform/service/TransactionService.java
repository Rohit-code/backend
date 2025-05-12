package com.analyticalplatform.service;

import com.analyticalplatform.dto.StockDTO;
import com.analyticalplatform.dto.TransactionDTO;
import com.analyticalplatform.model.StockTransaction;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.repository.StockTransactionRepository;
import com.analyticalplatform.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final StockTransactionRepository transactionRepository;
    private final UserStockRepository userStockRepository;
    private final StockService stockService;

    @Transactional
    public StockTransaction executeTransaction(User user, TransactionDTO transactionDTO) {
        // Get current stock price
        String symbol = transactionDTO.getSymbol();
        StockDTO stockDTO = stockService.getStockBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));

        // Calculate transaction amount
        BigDecimal price = stockDTO.getCurrentPrice();
        long quantity = transactionDTO.getQuantity(); // Use primitive long
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(quantity));

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
        return transactionRepository.save(transaction);
    }

    private void updateUserPortfolio(User user, StockTransaction transaction) {
        String symbol = transaction.getSymbol();
        long quantity = transaction.getQuantity(); // Use primitive long
        String transactionType = transaction.getTransactionType();

        Optional<UserStock> userStockOpt = userStockRepository.findByUserIdAndSymbol(user.getId(), symbol);

        if ("BUY".equals(transactionType)) {
            if (userStockOpt.isPresent()) {
                UserStock userStock = userStockOpt.get();

                // Update average buy price
                long oldQuantity = userStock.getQuantity(); // Use primitive long
                BigDecimal oldTotalValue = userStock.getAverageBuyPrice().multiply(BigDecimal.valueOf(oldQuantity));
                BigDecimal newValue = transaction.getPrice().multiply(BigDecimal.valueOf(quantity));
                long newQuantity = oldQuantity + quantity; // Use primitive long

                // Use RoundingMode instead of deprecated constants
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
                    throw new RuntimeException("Not enough stocks to sell");
                }

                // Update quantity
                long newQuantity = userStock.getQuantity() - quantity; // Use primitive long

                if (newQuantity > 0) {
                    userStock.setQuantity(newQuantity);
                    userStockRepository.save(userStock);
                } else {
                    // If quantity becomes 0, remove the entry
                    userStockRepository.delete(userStock);
                }
            } else {
                throw new RuntimeException("No stocks available to sell");
            }
        }
    }

    public List<StockTransaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionTimeDesc(userId);
    }

    public List<UserStock> getUserPortfolio(Long userId) {
        return userStockRepository.findByUserId(userId);
    }
}