package com.analyticalplatform.service;

import com.analyticalplatform.model.Stock;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.UserStock;
import com.analyticalplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioUpdateService {
    private final UserRepository userRepository;
    private final StockService stockService;
    private final TransactionService transactionService;

    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void updatePortfolioValues() {
        log.info("Scheduled portfolio value update starting");

        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                List<UserStock> portfolio = transactionService.getUserPortfolio(user.getId());

                if (!portfolio.isEmpty()) {
                    for (UserStock holding : portfolio) {
                        String symbol = holding.getSymbol();

                        // Update stock data if needed
                        stockService.getStockBySymbol(symbol)
                                .orElseGet(() -> stockService.updateStockData(symbol));
                    }

                    log.debug("Portfolio updated for user: {}", user.getUsername());
                }
            } catch (Exception e) {
                log.error("Error updating portfolio for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        log.info("Scheduled portfolio value update completed");
    }
}