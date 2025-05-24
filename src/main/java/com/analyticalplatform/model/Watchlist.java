package com.analyticalplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "watchlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    @CollectionTable(name = "watchlist_symbols", joinColumns = @JoinColumn(name = "watchlist_id"))
    @Column(name = "symbol")
    private Set<String> symbols = new HashSet<>();

    // Business logic methods
    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    public void removeSymbol(String symbol) {
        symbols.remove(symbol);
    }

    public boolean containsSymbol(String symbol) {
        return symbols.contains(symbol);
    }
}