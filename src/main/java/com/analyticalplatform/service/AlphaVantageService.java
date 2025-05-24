package com.analyticalplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageService {
    private final RestTemplate restTemplate;

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.url:https://www.alphavantage.co}")
    private String apiUrl;

    /**
     * Search for stocks by query
     */
    public Map<String, Object> searchStocks(String query) {
        String url = String.format("%s/query?function=SYMBOL_SEARCH&keywords=%s&apikey=%s",
                apiUrl, query, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get intraday time series data
     */
    public Map<String, Object> getTimeSeriesIntraday(String symbol, String interval, String outputSize) {
        String url = String.format("%s/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=%s&outputsize=%s&apikey=%s",
                apiUrl, symbol, interval, outputSize, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get daily time series data
     */
    public Map<String, Object> getTimeSeriesDaily(String symbol, String outputSize) {
        String url = String.format("%s/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=%s&apikey=%s",
                apiUrl, symbol, outputSize, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get weekly time series data
     */
    public Map<String, Object> getTimeSeriesWeekly(String symbol) {
        String url = String.format("%s/query?function=TIME_SERIES_WEEKLY&symbol=%s&apikey=%s",
                apiUrl, symbol, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get monthly time series data
     */
    public Map<String, Object> getTimeSeriesMonthly(String symbol) {
        String url = String.format("%s/query?function=TIME_SERIES_MONTHLY&symbol=%s&apikey=%s",
                apiUrl, symbol, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get company overview
     */
    public Map<String, Object> getCompanyOverview(String symbol) {
        String url = String.format("%s/query?function=OVERVIEW&symbol=%s&apikey=%s",
                apiUrl, symbol, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get global quote
     */
    public Map<String, Object> getGlobalQuote(String symbol) {
        String url = String.format("%s/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                apiUrl, symbol, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get top gainers and losers
     */
    public Map<String, Object> getTopGainersLosers() {
        String url = String.format("%s/query?function=TOP_GAINERS_LOSERS&apikey=%s",
                apiUrl, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get currency exchange rate
     */
    public Map<String, Object> getCurrencyExchangeRate(String fromCurrency, String toCurrency) {
        String url = String.format("%s/query?function=CURRENCY_EXCHANGE_RATE&from_currency=%s&to_currency=%s&apikey=%s",
                apiUrl, fromCurrency, toCurrency, apiKey);
        log.info("Calling Alpha Vantage API: {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Alpha Vantage API: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}