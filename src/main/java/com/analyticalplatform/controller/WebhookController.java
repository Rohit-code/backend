package com.analyticalplatform.controller;

import com.analyticalplatform.service.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Api(tags = "Webhook API", description = "API for external webhooks")
public class WebhookController {
    private final StockService stockService;

    @Value("${finnhub.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook/finnhub")
    @ApiOperation(value = "Handle Finnhub webhook")
    public ResponseEntity<?> handleFinnhubWebhook(
            @RequestHeader("X-Finnhub-Secret") String secret,
            @RequestBody WebhookPayload payload) {

        // Verify webhook secret
        if (!webhookSecret.equals(secret)) {
            return ResponseEntity.status(403).body("Invalid webhook secret");
        }

        // Process trade updates
        if ("trade".equals(payload.getType())) {
            for (WebhookPayload.TradeData trade : payload.getData()) {
                String symbol = trade.getS(); // Stock symbol
                stockService.updateStockData(symbol);
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }

    // Static class to make it accessible from the controller method
    @Data
    public static class WebhookPayload {
        private String type;
        private TradeData[] data;

        @Data
        public static class TradeData {
            private String s; // Symbol
            private double p; // Last price
            private long v; // Volume
        }
    }
}