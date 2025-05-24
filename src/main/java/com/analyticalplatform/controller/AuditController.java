package com.analyticalplatform.controller;

import com.analyticalplatform.model.AuditLog;
import com.analyticalplatform.service.AuditService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Audit API", description = "API for audit logs (admin only)")
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/user/{username}")
    @ApiOperation(value = "Get audit logs by username", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<AuditLog> logs = auditService.getUserAuditLogs(username, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/date-range")
    @ApiOperation(value = "Get audit logs by date range", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<AuditLog> logs = auditService.getAuditLogsByDateRange(start, end, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/action/{action}")
    @ApiOperation(value = "Get audit logs by action", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<AuditLog> logs = auditService.getAuditLogsByAction(action, pageable);

        return ResponseEntity.ok(logs);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(page, size, sort);
    }
}