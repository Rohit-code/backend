package com.analyticalplatform.controller;

import com.analyticalplatform.service.AlphaVantageService;
import com.analyticalplatform.service.ApiRateLimiterService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market Data API", description = "API for market data from Alpha Vantage")
@Slf4j
public class MarketDataController {
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;

    @GetMapping("/search")
    @ApiOperation(value = "Search for stocks", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> searchStocks(@RequestParam String query) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.searchStocks(query));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/time-series/intraday/{symbol}")
    @ApiOperation(value = "Get intraday time series", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getTimeSeriesIntraday(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5min") String interval,
            @RequestParam(defaultValue = "compact") String outputSize) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getTimeSeriesIntraday(symbol, interval, outputSize));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/time-series/daily/{symbol}")
    @ApiOperation(value = "Get daily time series", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getTimeSeriesDaily(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "compact") String outputSize) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getTimeSeriesDaily(symbol, outputSize));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/time-series/weekly/{symbol}")
    @ApiOperation(value = "Get weekly time series", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getTimeSeriesWeekly(@PathVariable String symbol) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getTimeSeriesWeekly(symbol));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/time-series/monthly/{symbol}")
    @ApiOperation(value = "Get monthly time series", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getTimeSeriesMonthly(@PathVariable String symbol) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getTimeSeriesMonthly(symbol));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/quote/{symbol}")
    @ApiOperation(value = "Get global quote", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getGlobalQuote(@PathVariable String symbol) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getGlobalQuote(symbol));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/company/{symbol}")
    @ApiOperation(value = "Get company overview", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getCompanyOverview(@PathVariable String symbol) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getCompanyOverview(symbol));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/top-gainers-losers")
    @ApiOperation(value = "Get top gainers and losers", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getTopGainersLosers() {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getTopGainersLosers());
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }

    @GetMapping("/forex/{fromCurrency}/{toCurrency}")
    @ApiOperation(value = "Get currency exchange rate", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getCurrencyExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        try {
            apiRateLimiterService.acquirePermit();
            return ResponseEntity.ok(alphaVantageService.getCurrencyExchangeRate(fromCurrency, toCurrency));
        } catch (InterruptedException e) {
            log.error("API rate limit error", e);
            return ResponseEntity.status(429).body("API rate limit exceeded. Please try again later.");
        }
    }
}