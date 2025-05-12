package com.analyticalplatform.controller;

import com.analyticalplatform.dto.StockDTO;
import com.analyticalplatform.service.StockService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock API", description = "API for stock data operations")
public class StockController {
    private final StockService stockService;

    @GetMapping
    @ApiOperation(value = "Get all stocks", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }

    @GetMapping("/{symbol}")
    @ApiOperation(value = "Get stock by symbol", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<StockDTO> getStockBySymbol(@PathVariable String symbol) {
        return stockService.getStockBySymbol(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top-performers")
    @ApiOperation(value = "Get top performing stocks", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<StockDTO>> getTopPerformers() {
        return ResponseEntity.ok(stockService.getTopPerformers());
    }

    @GetMapping("/worst-performers")
    @ApiOperation(value = "Get worst performing stocks", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<StockDTO>> getWorstPerformers() {
        return ResponseEntity.ok(stockService.getWorstPerformers());
    }

    @PostMapping("/update/{symbol}")
    @ApiOperation(value = "Update stock data for a symbol (admin only)", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<StockDTO> updateStockData(@PathVariable String symbol) {
        StockDTO updatedStock = stockService.updateStockData(symbol);
        if (updatedStock != null) {
            return ResponseEntity.ok(updatedStock);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}