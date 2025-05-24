package com.analyticalplatform.service;

import com.analyticalplatform.dto.NotificationDTO;
import com.analyticalplatform.dto.StockDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void sendStockUpdate(StockDTO stockDTO) {
        messagingTemplate.convertAndSend("/topic/stocks/" + stockDTO.getSymbol(), stockDTO);
    }

    public void sendNotification(String username, NotificationDTO notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }
}