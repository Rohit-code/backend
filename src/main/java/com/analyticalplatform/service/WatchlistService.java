package com.analyticalplatform.service;

import com.analyticalplatform.dto.WatchlistDTO;
import com.analyticalplatform.exception.ResourceNotFoundException;
import com.analyticalplatform.model.User;
import com.analyticalplatform.model.Watchlist;
import com.analyticalplatform.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {
    private final WatchlistRepository watchlistRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WatchlistDTO> getUserWatchlists(User user) {
        return watchlistRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WatchlistDTO getWatchlistById(User user, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found with id: " + watchlistId));

        // Security check - ensure the watchlist belongs to the user
        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Watchlist not found with id: " + watchlistId);
        }

        return mapToDTO(watchlist);
    }

    @Transactional
    public WatchlistDTO createWatchlist(User user, WatchlistDTO watchlistDTO) {
        // Check if name already exists for this user
        watchlistRepository.findByUserIdAndName(user.getId(), watchlistDTO.getName())
                .ifPresent(w -> {
                    throw new RuntimeException("Watchlist with name '" + watchlistDTO.getName() + "' already exists");
                });

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .name(watchlistDTO.getName())
                .symbols(watchlistDTO.getSymbols())
                .build();

        watchlist = watchlistRepository.save(watchlist);

        auditService.logAction(user.getUsername(), "CREATE", "Watchlist", watchlist.getId().toString(),
                "Created watchlist: " + watchlist.getName() + " with " + watchlist.getSymbols().size() + " symbols");

        return mapToDTO(watchlist);
    }

    @Transactional
    public WatchlistDTO updateWatchlist(User user, Long watchlistId, WatchlistDTO watchlistDTO) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found with id: " + watchlistId));

        // Security check - ensure the watchlist belongs to the user
        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Watchlist not found with id: " + watchlistId);
        }

        // Update name if it changed
        if (!watchlist.getName().equals(watchlistDTO.getName())) {
            // Check if new name already exists
            watchlistRepository.findByUserIdAndName(user.getId(), watchlistDTO.getName())
                    .ifPresent(w -> {
                        if (!w.getId().equals(watchlistId)) {
                            throw new RuntimeException("Watchlist with name '" + watchlistDTO.getName() + "' already exists");
                        }
                    });

            watchlist.setName(watchlistDTO.getName());
        }

        // Update symbols
        watchlist.setSymbols(watchlistDTO.getSymbols());

        watchlist = watchlistRepository.save(watchlist);

        auditService.logAction(user.getUsername(), "UPDATE", "Watchlist", watchlist.getId().toString(),
                "Updated watchlist: " + watchlist.getName());

        return mapToDTO(watchlist);
    }

    @Transactional
    public void deleteWatchlist(User user, Long watchlistId) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found with id: " + watchlistId));

        // Security check - ensure the watchlist belongs to the user
        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Watchlist not found with id: " + watchlistId);
        }

        watchlistRepository.delete(watchlist);

        auditService.logAction(user.getUsername(), "DELETE", "Watchlist", watchlistId.toString(),
                "Deleted watchlist: " + watchlist.getName());
    }

    @Transactional
    public WatchlistDTO addSymbol(User user, Long watchlistId, String symbol) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found with id: " + watchlistId));

        // Security check - ensure the watchlist belongs to the user
        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Watchlist not found with id: " + watchlistId);
        }

        watchlist.addSymbol(symbol);
        watchlist = watchlistRepository.save(watchlist);

        auditService.logAction(user.getUsername(), "ADD_SYMBOL", "Watchlist", watchlist.getId().toString(),
                "Added symbol " + symbol + " to watchlist: " + watchlist.getName());

        return mapToDTO(watchlist);
    }

    @Transactional
    public WatchlistDTO removeSymbol(User user, Long watchlistId, String symbol) {
        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist not found with id: " + watchlistId));

        // Security check - ensure the watchlist belongs to the user
        if (!watchlist.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Watchlist not found with id: " + watchlistId);
        }

        watchlist.removeSymbol(symbol);
        watchlist = watchlistRepository.save(watchlist);

        auditService.logAction(user.getUsername(), "REMOVE_SYMBOL", "Watchlist", watchlist.getId().toString(),
                "Removed symbol " + symbol + " from watchlist: " + watchlist.getName());

        return mapToDTO(watchlist);
    }

    private WatchlistDTO mapToDTO(Watchlist watchlist) {
        return WatchlistDTO.builder()
                .id(watchlist.getId())
                .name(watchlist.getName())
                .symbols(watchlist.getSymbols())
                .build();
    }
}