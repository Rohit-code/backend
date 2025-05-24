package com.analyticalplatform.service;

import com.analyticalplatform.dto.PriceAlertDTO;
import com.analyticalplatform.exception.ResourceNotFoundException;
import com.analyticalplatform.model.PriceAlert;
import com.analyticalplatform.model.Stock;
import com.analyticalplatform.model.User;
import com.analyticalplatform.repository.PriceAlertRepository;
import com.analyticalplatform.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {
    private final PriceAlertRepository priceAlertRepository;
    private final StockRepository stockRepository;
    private final AlphaVantageService alphaVantageService;
    private final ApiRateLimiterService apiRateLimiterService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final StockService stockService;

    @Transactional(readOnly = true)
    public List<PriceAlertDTO> getUserAlerts(User user) {
        return priceAlertRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PriceAlertDTO> getUserAlertsForSymbol(User user, String symbol) {
        return priceAlertRepository.findByUserIdAndSymbol(user.getId(), symbol).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PriceAlertDTO createAlert(User user, PriceAlertDTO alertDTO) {
        // Validate alertType
        if (!"ABOVE".equals(alertDTO.getAlertType()) && !"BELOW".equals(alertDTO.getAlertType())) {
            throw new IllegalArgumentException("Alert type must be 'ABOVE' or 'BELOW'");
        }

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .symbol(alertDTO.getSymbol().toUpperCase())
                .targetPrice(alertDTO.getTargetPrice())
                .alertType(alertDTO.getAlertType())
                .triggered(false)
                .createdAt(LocalDateTime.now())
                .build();

        alert = priceAlertRepository.save(alert);

        auditService.logAction(user.getUsername(), "CREATE", "PriceAlert", alert.getId().toString(),
                "Created price alert for " + alert.getSymbol() + " " + alert.getAlertType() + " " + alert.getTargetPrice());

        return mapToDTO(alert);
    }

    @Transactional
    public void deleteAlert(User user, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with id: " + alertId));

        // Security check - ensure the alert belongs to the user
        if (!alert.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Price alert not found with id: " + alertId);
        }

        priceAlertRepository.delete(alert);

        auditService.logAction(user.getUsername(), "DELETE", "PriceAlert", alertId.toString(),
                "Deleted price alert for " + alert.getSymbol());
    }

    @Scheduled(fixedRate = 300000) // Check every 5 minutes
    @Transactional
    public void checkAlerts() {
        log.info("Checking price alerts");

        List<PriceAlert> activeAlerts = priceAlertRepository.findAllActiveAlerts();
        if (activeAlerts.isEmpty()) {
            log.info("No active alerts to check");
            return;
        }

        // Group alerts by symbol to minimize API calls
        Map<String, List<PriceAlert>> alertsBySymbol = activeAlerts.stream()
                .collect(Collectors.groupingBy(PriceAlert::getSymbol));

        for (Map.Entry<String, List<PriceAlert>> entry : alertsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<PriceAlert> symbolAlerts = entry.getValue();

            try {
                // Get current price - first try from repository
                BigDecimal currentPrice = null;

                Stock stock = stockRepository.findById(symbol).orElse(null);
                if (stock != null && stock.getLastUpdated() != null &&
                        stock.getLastUpdated().isAfter(LocalDateTime.now().minusMinutes(5))) {
                    // Use cached price if less than 5 minutes old
                    currentPrice = stock.getCurrentPrice();
                    log.info("Using cached price for {}: ${}", symbol, currentPrice);
                } else {
                    // Get fresh price from API
                    apiRateLimiterService.acquirePermit();
                    Map<String, Object> quoteData = alphaVantageService.getGlobalQuote(symbol);

                    if (quoteData != null && quoteData.containsKey("Global Quote")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> quote = (Map<String, Object>) quoteData.get("Global Quote");
                        String priceStr = (String) quote.get("05. price");

                        if (priceStr != null) {
                            currentPrice = new BigDecimal(priceStr);

                            // Update stock in database
                            if (stock == null) {
                                stock = new Stock();
                                stock.setSymbol(symbol);
                            }

                            stock.setCurrentPrice(currentPrice);
                            stock.setLastUpdated(LocalDateTime.now());
                            stockRepository.save(stock);

                            log.info("Updated price for {}: ${}", symbol, currentPrice);
                        }
                    }
                }

                if (currentPrice != null) {
                    // Check each alert for this symbol
                    for (PriceAlert alert : symbolAlerts) {
                        if (alert.shouldTrigger(currentPrice)) {
                            log.info("Alert triggered for {}: {} ${}",
                                    alert.getSymbol(), alert.getAlertType(), alert.getTargetPrice());

                            alert.markAsTriggered();
                            priceAlertRepository.save(alert);

                            // Send notification
                            notificationService.sendPriceAlert(alert, currentPrice);

                            auditService.logAction(alert.getUser().getUsername(), "ALERT_TRIGGERED",
                                    "PriceAlert", alert.getId().toString(),
                                    "Price alert triggered for " + alert.getSymbol() + " at " + currentPrice);
                        }
                    }
                } else {
                    log.warn("Could not get current price for {}", symbol);
                }
            } catch (InterruptedException e) {
                log.error("API rate limit reached when checking alerts for {}", symbol, e);
            } catch (Exception e) {
                log.error("Error checking alerts for {}: {}", symbol, e.getMessage());
            }
        }
    }

    private PriceAlertDTO mapToDTO(PriceAlert alert) {
        return PriceAlertDTO.builder()
                .id(alert.getId())
                .symbol(alert.getSymbol())
                .targetPrice(alert.getTargetPrice())
                .alertType(alert.getAlertType())
                .triggered(alert.isTriggered())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}