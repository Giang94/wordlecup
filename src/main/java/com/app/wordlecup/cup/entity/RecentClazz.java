package com.app.wordlecup.cup.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recent_clazz")
@Getter
@Setter
public class RecentClazz {

    public static final int MAX_RECENT_GAMES = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Column(nullable = false)
    private boolean isWin;

    @Column(nullable = false)
    private Instant usedAt;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @PrePersist
    void onUse() {
        this.usedAt = Instant.now();
    }
}
