package com.app.wordlecup.model;

import java.util.List;

public record RecentGameResp(
        List<String> recentGames,
        int streak
) {
}