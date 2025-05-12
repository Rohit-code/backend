package com.analyticalplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinnhubService {
    private final RestTemplate restTemplate;

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    @Value("${finnhub.api.url}")
    private String finnhubApiUrl;

    /**
     * Search for stocks by query
     */
    public Map<String, Object> searchStocks(String query) {
        String url = String.format("%s/search?q=%s&token=%s", finnhubApiUrl, query, finnhubApiKey);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return response.getBody();
    }

    /**
     * Get company news
     */
    public Object[] getCompanyNews(String symbol, String from, String to) {
        String url = String.format("%s/company-news?symbol=%s&from=%s&to=%s&token=%s",
                finnhubApiUrl, symbol, from, to, finnhubApiKey);
        return restTemplate.getForObject(url, Object[].class);
    }

    /**
     * Get stock candles (historical data)
     */
    public Map<String, Object> getStockCandles(String symbol, String resolution, long from, long to) {
        String url = String.format("%s/stock/candle?symbol=%s&resolution=%s&from=%d&to=%d&token=%s",
                finnhubApiUrl, symbol, resolution, from, to, finnhubApiKey);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return response.getBody();
    }

    /**
     * Get company financial metrics
     */
    public Map<String, Object> getCompanyMetrics(String symbol) {
        String url = String.format("%s/stock/metric?symbol=%s&metric=all&token=%s",
                finnhubApiUrl, symbol, finnhubApiKey);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return response.getBody();
    }
}