package com.analyticalplatform.service;

import com.analyticalplatform.dto.StockDTO;
import com.analyticalplatform.model.Stock;
import com.analyticalplatform.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
    private final StockRepository stockRepository;
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;

    public List<StockDTO> getAllStocks() {
        return stockRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public Optional<StockDTO> getStockBySymbol(String symbol) {
        return stockRepository.findById(symbol)
                .map(this::mapToDTO);
    }

    public List<StockDTO> getTopPerformers() {
        return stockRepository.findTopPerformers().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<StockDTO> getWorstPerformers() {
        return stockRepository.findWorstPerformers().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Scheduled(fixedRate = 1800000) // Update every 30 minutes instead of 5 minutes
    public void updateTopStocks() {
        try {
            log.info("Starting scheduled update of top stocks");

            // Limit to only 2 stocks per scheduled run to stay within limits
            List<String> priorityStocks = Arrays.asList("AAPL", "MSFT");

            for (String symbol : priorityStocks) {
                try {
                    log.info("Updating priority stock: {}", symbol);
                    apiRateLimiterService.acquirePermit();
                    updateStockData(symbol);

                    // Add small delay between updates
                    Thread.sleep(5000); // 5 second delay

                } catch (InterruptedException e) {
                    log.error("API rate limit reached during scheduled update for {}", symbol, e);
                    break; // Stop processing more stocks if rate limit hit
                } catch (Exception e) {
                    log.error("Error updating stock {}: {}", symbol, e.getMessage());
                    // Continue with next stock
                }
            }

            log.info("Scheduled stock update completed");

        } catch (Exception e) {
            log.error("Error in scheduled stock update", e);
        }
    }

    public StockDTO updateStockData(String symbol) {
        try {
            // Acquire API permit
            apiRateLimiterService.acquirePermit();

            // Get quote data
            Map<String, Object> quoteData = alphaVantageService.getGlobalQuote(symbol);

            if (quoteData != null && quoteData.containsKey("Global Quote")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> quote = (Map<String, Object>) quoteData.get("Global Quote");

                // Get or create stock entity
                Stock stock = stockRepository.findById(symbol).orElse(new Stock());
                stock.setSymbol(symbol);

                // Get company info if needed
                if (stock.getCompanyName() == null || stock.getCompanyName().isEmpty()) {
                    try {
                        apiRateLimiterService.acquirePermit();
                        Map<String, Object> overview = alphaVantageService.getCompanyOverview(symbol);
                        if (overview != null && overview.containsKey("Name")) {
                            stock.setCompanyName((String) overview.get("Name"));
                        } else {
                            stock.setCompanyName(symbol); // Fall back to using symbol as name
                        }
                    } catch (InterruptedException e) {
                        // Just use symbol name if we can't get company info
                        log.warn("Could not get company info for {}: {}", symbol, e.getMessage());
                        stock.setCompanyName(symbol);
                    }
                }

                // Extract price info
                String currentPriceStr = (String) quote.get("05. price");
                String previousCloseStr = (String) quote.get("08. previous close");
                String volumeStr = (String) quote.get("06. volume");

                if (currentPriceStr != null && previousCloseStr != null && volumeStr != null) {
                    try {
                        BigDecimal currentPrice = new BigDecimal(currentPriceStr);
                        BigDecimal previousClose = new BigDecimal(previousCloseStr);
                        Long volume = Long.parseLong(volumeStr);

                        // Calculate percent change
                        BigDecimal percentChange = BigDecimal.ZERO;
                        if (previousClose.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal change = currentPrice.subtract(previousClose);
                            percentChange = change.divide(previousClose, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                        }

                        // Update stock data
                        stock.setCurrentPrice(currentPrice);
                        stock.setPreviousClose(previousClose);
                        stock.setPercentChange(percentChange);
                        stock.setVolume(volume);
                        stock.setLastUpdated(LocalDateTime.now());

                        // Save to database
                        stock = stockRepository.save(stock);
                        log.info("Updated stock data for {}: ${}, {}%", symbol, currentPrice, percentChange);

                        return mapToDTO(stock);
                    } catch (NumberFormatException e) {
                        log.error("Error parsing numeric values for stock {}: {}", symbol, e.getMessage());
                    }
                } else {
                    log.error("Missing required quote data for stock {}", symbol);
                }
            } else {
                log.error("Invalid quote data response for stock {}: {}", symbol, quoteData);
            }
        } catch (InterruptedException e) {
            log.error("API rate limit reached for stock {}", symbol, e);
        } catch (Exception e) {
            log.error("Error updating stock data for {}: {}", symbol, e.getMessage());
        }

        return null;
    }

    private StockDTO mapToDTO(Stock stock) {
        return StockDTO.builder()
                .symbol(stock.getSymbol())
                .companyName(stock.getCompanyName())
                .currentPrice(stock.getCurrentPrice())
                .previousClose(stock.getPreviousClose())
                .percentChange(stock.getPercentChange())
                .volume(stock.getVolume())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }
}