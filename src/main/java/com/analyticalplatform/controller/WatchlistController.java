package com.analyticalplatform.controller;

import com.analyticalplatform.dto.WatchlistDTO;
import com.analyticalplatform.model.User;
import com.analyticalplatform.service.UserService;
import com.analyticalplatform.service.WatchlistService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
@Tag(name = "Watchlist API", description = "API for watchlist operations")
public class WatchlistController {
    private final WatchlistService watchlistService;
    private final UserService userService;

    @GetMapping
    @ApiOperation(value = "Get all user watchlists", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<List<WatchlistDTO>> getAllWatchlists() {
        User user = getCurrentUser();
        List<WatchlistDTO> watchlists = watchlistService.getUserWatchlists(user);
        return ResponseEntity.ok(watchlists);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get watchlist by ID", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<WatchlistDTO> getWatchlist(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            WatchlistDTO watchlist = watchlistService.getWatchlistById(user, id);
            return ResponseEntity.ok(watchlist);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @ApiOperation(value = "Create a new watchlist", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> createWatchlist(@Valid @RequestBody WatchlistDTO watchlistDTO) {
        try {
            User user = getCurrentUser();
            WatchlistDTO createdWatchlist = watchlistService.createWatchlist(user, watchlistDTO);
            return ResponseEntity.ok(createdWatchlist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Update a watchlist", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> updateWatchlist(
            @PathVariable Long id,
            @Valid @RequestBody WatchlistDTO watchlistDTO) {
        try {
            User user = getCurrentUser();
            WatchlistDTO updatedWatchlist = watchlistService.updateWatchlist(user, id, watchlistDTO);
            return ResponseEntity.ok(updatedWatchlist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a watchlist", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> deleteWatchlist(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            watchlistService.deleteWatchlist(user, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/symbols/{symbol}")
    @ApiOperation(value = "Add a symbol to watchlist", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> addSymbol(@PathVariable Long id, @PathVariable String symbol) {
        try {
            User user = getCurrentUser();
            WatchlistDTO updatedWatchlist = watchlistService.addSymbol(user, id, symbol.toUpperCase());
            return ResponseEntity.ok(updatedWatchlist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/symbols/{symbol}")
    @ApiOperation(value = "Remove a symbol from watchlist", authorizations = {@Authorization(value = "JWT")})
    public ResponseEntity<?> removeSymbol(@PathVariable Long id, @PathVariable String symbol) {
        try {
            User user = getCurrentUser();
            WatchlistDTO updatedWatchlist = watchlistService.removeSymbol(user, id, symbol.toUpperCase());
            return ResponseEntity.ok(updatedWatchlist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}