package com.analyticalplatform.service;

import com.analyticalplatform.dto.NotificationDTO;
import com.analyticalplatform.model.Notification;
import com.analyticalplatform.model.PriceAlert;
import com.analyticalplatform.model.User;
import com.analyticalplatform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendPriceAlert(PriceAlert alert, BigDecimal currentPrice) {
        User user = alert.getUser();
        String message = String.format("Price Alert: %s has reached $%s, %s your target price of $%s",
                alert.getSymbol(),
                currentPrice.toString(),
                "ABOVE".equals(alert.getAlertType()) ? "above" : "below",
                alert.getTargetPrice().toString());

        Notification notification = Notification.builder()
                .user(user)
                .type("PRICE_ALERT")
                .message(message)
                .read(false)
                .timestamp(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Price alert notification sent to user {}: {}", user.getUsername(), message);
    }

    @Transactional
    public void sendTransactionNotification(User user, String symbol, long quantity, BigDecimal price, String type) {
        String message = String.format("Transaction Confirmation: %s %d shares of %s at $%s per share",
                "BUY".equals(type) ? "Bought" : "Sold",
                quantity,
                symbol,
                price.toString());

        Notification notification = Notification.builder()
                .user(user)
                .type("TRANSACTION")
                .message(message)
                .read(false)
                .timestamp(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Transaction notification sent to user {}: {}", user.getUsername(), message);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(User user) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotificationsPaged(User user, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(user.getId(), pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(User user) {
        return notificationRepository.findUnreadByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify user owns this notification
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification not found");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return mapToDTO(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findUnreadByUserId(user.getId());
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .timestamp(notification.getTimestamp())
                .build();
    }
}