package com.analyticalplatform.controller;

import com.analyticalplatform.dto.NotificationDTO;
import com.analyticalplatform.model.User;
import com.analyticalplatform.service.NotificationService;
import com.analyticalplatform.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API", description = "API for user notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @ApiOperation(value = "Get user notifications", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = getCurrentUser();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<NotificationDTO> notifications = notificationService.getUserNotificationsPaged(user, pageRequest);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    @ApiOperation(value = "Get unread notifications", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        User user = getCurrentUser();
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(user);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @ApiOperation(value = "Get unread notification count", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = getCurrentUser();
        long count = notificationService.getUnreadCount(user);

        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    @ApiOperation(value = "Mark notification as read", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            NotificationDTO notification = notificationService.markAsRead(id, user);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/read-all")
    @ApiOperation(value = "Mark all notifications as read", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> markAllAsRead() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}