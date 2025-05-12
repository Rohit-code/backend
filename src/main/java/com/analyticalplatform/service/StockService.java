package com.analyticalplatform.service;

import com.analyticalplatform.dto.StockDTO;
import com.analyticalplatform.model.Stock;
import com.analyticalplatform.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    @Value("${finnhub.api.url}")
    private String finnhubApiUrl;

    public List<StockDTO> getAllStocks() {
        return stockRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList(); // Using toList() instead of collect(Collectors.toList())
    }

    public Optional<StockDTO> getStockBySymbol(String symbol) {
        // Stock entity has String ID
        return stockRepository.findById(symbol) // This is correct
                .map(this::mapToDTO);
    }

    public List<StockDTO> getTopPerformers() {
        return stockRepository.findTopPerformers().stream()
                .map(this::mapToDTO)
                .toList(); // Using toList() instead of collect(Collectors.toList())
    }

    public List<StockDTO> getWorstPerformers() {
        return stockRepository.findWorstPerformers().stream()
                .map(this::mapToDTO)
                .toList(); // Using toList() instead of collect(Collectors.toList())
    }

    @Scheduled(fixedRate = 60000) // Update every minute
    public void updateStockPrices() {
        // Get all stock symbols from DB
        List<String> symbols = stockRepository.findAll().stream()
                .map(Stock::getSymbol)
                .toList(); // Using toList() instead of collect(Collectors.toList())

        // Update each stock with latest data from Finnhub
        for (String symbol : symbols) {
            updateStockData(symbol);
        }
    }

    public StockDTO updateStockData(String symbol) {
        String url = String.format("%s/quote?symbol=%s&token=%s", finnhubApiUrl, symbol, finnhubApiKey);

        // Call Finnhub API
        FinnhubQuoteResponse response = restTemplate.getForObject(url, FinnhubQuoteResponse.class);

        if (response != null) {
            // Stock entity has String ID
            Stock stock = stockRepository.findById(symbol).orElse(new Stock()); // This is correct
            stock.setSymbol(symbol);
            stock.setCurrentPrice(BigDecimal.valueOf(response.getC()));
            stock.setPreviousClose(BigDecimal.valueOf(response.getPc()));

            // Calculate percent change
            if (stock.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = stock.getCurrentPrice().subtract(stock.getPreviousClose());
                // Use RoundingMode instead of deprecated constants
                BigDecimal percentChange = change.divide(stock.getPreviousClose(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                stock.setPercentChange(percentChange);
            }

            stock.setVolume(response.getT());
            stock.setLastUpdated(LocalDateTime.now());

            stock = stockRepository.save(stock);
            return mapToDTO(stock);
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

    // Use Lombok for getter/setter
    @lombok.Data
    private static class FinnhubQuoteResponse {
        private double c;  // Current price
        private double pc; // Previous close
        private long t;    // Volume
    }
}