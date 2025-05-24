package com.analyticalplatform.service;

import com.analyticalplatform.model.Stock;
import com.analyticalplatform.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyInfoService {
    private final StockRepository stockRepository;
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;

    /**
     * Update company information using Alpha Vantage
     */
    public void updateCompanyInfo(String symbol) {
        try {
            // Acquire API rate limit permit
            apiRateLimiterService.acquirePermit();

            // Call Alpha Vantage API
            Map<String, Object> companyData = alphaVantageService.getCompanyOverview(symbol);

            if (companyData != null && companyData.containsKey("Name")) {
                String companyName = (String) companyData.get("Name");
                String sector = (String) companyData.get("Sector");
                String industry = (String) companyData.get("Industry");
                String description = (String) companyData.get("Description");

                // Find or create stock entity
                Optional<Stock> stockOpt = stockRepository.findById(symbol);
                Stock stock;

                if (stockOpt.isPresent()) {
                    stock = stockOpt.get();
                } else {
                    stock = new Stock();
                    stock.setSymbol(symbol);
                    // Initialize with default values
                    stock.setCurrentPrice(null);
                    stock.setPreviousClose(null);
                    stock.setPercentChange(null);
                    stock.setVolume(0L);
                }

                // Update company info
                stock.setCompanyName(companyName);

                // Add additional fields if you want to extend your Stock model
                // For now just log them
                log.info("Updated company info for {}: {} ({})", symbol, companyName, sector);

                // Save to repository
                stockRepository.save(stock);
            } else {
                log.warn("No company data found for symbol: {}", symbol);
            }
        } catch (InterruptedException e) {
            log.error("API rate limit error when updating company info for {}", symbol, e);
        } catch (Exception e) {
            log.error("Error updating company info for {}", symbol, e);
        }
    }
}