package com.analyticalplatform.config;

import com.analyticalplatform.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StockDataInitializer {

    private final StockService stockService;

    @Bean
    @Profile("!test") // Skip in test profile
    public CommandLineRunner initializeStockData() {
        return args -> {
            log.info("Initializing stock data...");

            // Reduced list to stay within API limits
            List<String> initialStocks = Arrays.asList(
                    "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA"
            );

            // Use scheduled executor with longer delays to respect API rate limits
            try (var executorService = Executors.newScheduledThreadPool(1)) {
                AtomicInteger counter = new AtomicInteger(0);

                for (String symbol : initialStocks) {
                    // Add 20-second delay between API calls (3 calls per minute)
                    executorService.schedule(() -> {
                        try {
                            log.info("Initializing stock data for {} ({}/{})",
                                    symbol, counter.get() + 1, initialStocks.size());
                            stockService.updateStockData(symbol);

                            // Log progress
                            int processed = counter.incrementAndGet();
                            if (processed == initialStocks.size()) {
                                log.info("Stock data initialization complete.");
                            }
                        } catch (Exception e) {
                            log.error("Error initializing stock data for {}: {}", symbol, e.getMessage());
                            // Continue with next stock even if one fails
                        }
                    }, (long) counter.get() * 20L, TimeUnit.SECONDS);
                }

                // Shutdown gracefully
                executorService.shutdown();

                // Wait for all tasks to complete, or timeout after 10 minutes
                boolean completed = executorService.awaitTermination(10, TimeUnit.MINUTES);
                if (completed) {
                    log.info("All stock initialization tasks completed successfully");
                } else {
                    log.warn("Stock initialization timed out before all tasks completed");
                    // Attempt to cancel any remaining tasks
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Stock data initialization interrupted", e);
            }
        };
    }
}