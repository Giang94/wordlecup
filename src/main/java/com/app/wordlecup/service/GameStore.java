package com.app.wordlecup.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStore {

    public static final int MAX_ATTEMPTS = 6;
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public void createGame(String gameId, String answer) {
        games.put(gameId, new Game(answer));
    }

    // Get answer for a game
    public String getAnswer(String gameId) {
        Game game = games.get(gameId);
        return game != null ? game.answer : null;
    }

    // Remove a finished game
    public void removeGame(String gameId) {
        games.remove(gameId);
    }

    // Get number of attempts
    public int getAttempts(String gameId) {
        Game game = games.get(gameId);
        return game != null ? game.attempts : 0;
    }

    // Increment or set attempts
    public void setAttempts(String gameId, int attempts) {
        Game game = games.get(gameId);
        if (game != null) {
            game.attempts = attempts;
        }
    }

    private static class Game {
        String answer;
        int attempts;

        Game(String answer) {
            this.answer = answer;
            this.attempts = 0;
        }
    }
}
