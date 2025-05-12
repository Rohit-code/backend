package com.analyticalplatform.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics API", description = "API for application metrics")
public class MetricsController {
    private final MeterRegistry meterRegistry;

    // Counters for different API endpoints
    private Counter getStockCounter;
    private Counter getTransactionCounter;

    // Initialize counters
    public void initCounters() {
        // Create counters if they don't exist
        if (getStockCounter == null) {
            getStockCounter = Counter.builder("api.stock.requests")
                    .description("Number of stock API requests")
                    .register(meterRegistry);
        }

        if (getTransactionCounter == null) {
            getTransactionCounter = Counter.builder("api.transaction.requests")
                    .description("Number of transaction API requests")
                    .register(meterRegistry);
        }
    }

    @GetMapping("/increase-stock-counter")
    @ApiOperation(value = "Increase stock counter", authorizations = {@Authorization(value = "JWT")})
    public void increaseStockCounter() {
        initCounters();
        getStockCounter.increment();
    }

    @GetMapping("/increase-transaction-counter")
    @ApiOperation(value = "Increase transaction counter", authorizations = {@Authorization(value = "JWT")})
    public void increaseTransactionCounter() {
        initCounters();
        getTransactionCounter.increment();
    }
}