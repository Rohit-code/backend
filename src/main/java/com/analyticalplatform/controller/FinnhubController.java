package com.analyticalplatform.controller;

import com.analyticalplatform.service.FinnhubService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/finnhub")
@RequiredArgsConstructor
@Tag(name = "Finnhub API", description = "API for Finnhub data")
public class FinnhubController {
    private final FinnhubService finnhubService;

    @GetMapping("/search")
    @ApiOperation(value = "Search for stocks", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> searchStocks(@RequestParam String query) {
        return ResponseEntity.ok(finnhubService.searchStocks(query));
    }

    @GetMapping("/company-news/{symbol}")
    @ApiOperation(value = "Get company news", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getCompanyNews(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(finnhubService.getCompanyNews(
                symbol,
                from.toString(),
                to.toString()));
    }

    @GetMapping("/stock/candle/{symbol}")
    @ApiOperation(value = "Get stock candles", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getStockCandles(
            @PathVariable String symbol,
            @RequestParam String resolution,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        long fromTimestamp = from.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

        return ResponseEntity.ok(finnhubService.getStockCandles(symbol, resolution, fromTimestamp, toTimestamp));
    }

    @GetMapping("/stock/metrics/{symbol}")
    @ApiOperation(value = "Get company metrics", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> getCompanyMetrics(@PathVariable String symbol) {
        return ResponseEntity.ok(finnhubService.getCompanyMetrics(symbol));
    }
}