package com.analyticalplatform.service;

import com.analyticalplatform.model.Stock;
import com.analyticalplatform.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyInfoService {
    private final StockRepository stockRepository;
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    @Value("${finnhub.api.url}")
    private String finnhubApiUrl;

    public void updateCompanyInfo(String symbol) {
        String url = String.format("%s/stock/profile2?symbol=%s&token=%s", finnhubApiUrl, symbol, finnhubApiKey);

        // Call Finnhub API
        CompanyProfile response = restTemplate.getForObject(url, CompanyProfile.class);

        if (response != null) {
            // Stock entity has String ID, so this should work
            Optional<Stock> stockOpt = stockRepository.findById(symbol); // This is correct

            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                stock.setCompanyName(response.getName());
                stockRepository.save(stock);
            } else {
                Stock stock = new Stock();
                stock.setSymbol(symbol);
                stock.setCompanyName(response.getName());
                stockRepository.save(stock);
            }
        }
    }

    // Use Lombok for getters and setters
    @lombok.Data
    private static class CompanyProfile {
        private String name;
    }
}