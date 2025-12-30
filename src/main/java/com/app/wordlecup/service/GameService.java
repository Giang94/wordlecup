package com.app.wordlecup.service;

import com.app.wordlecup.model.Messages;
import com.app.wordlecup.model.RecentGame;
import com.app.wordlecup.model.RecentGameResp;
import com.app.wordlecup.model.Word;
import com.app.wordlecup.repo.RecentGameRepository;
import com.app.wordlecup.repo.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static com.app.wordlecup.model.RecentGame.MAX_RECENT_GAMES;

@Service
public class GameService {

    private final WordRepository wordRepository;
    private final RecentGameRepository recentGameRepository;
    private final GameStore gameStore;

    public GameService(WordRepository wordRepository, RecentGameRepository recentGameRepository, GameStore gameStore) {
        this.wordRepository = wordRepository;
        this.recentGameRepository = recentGameRepository;
        this.gameStore = gameStore;
    }

    @Transactional(readOnly = true)
    public boolean isValidWord(String word) {
        return wordRepository.existsByWord(word.toLowerCase());
    }

    public Map<String, Object> guess(String gameId, String guessInput) {
        String answer = gameStore.getAnswer(gameId);
        String guess = guessInput.toLowerCase();

        if (answer == null) {
            return Map.of("error", "Invalid game");
        }

        if (!isValidWord(guess)) {
            return Map.of("error", Messages.randomNotAWordMessage());
        }

        int attempts = gameStore.getAttempts(gameId);
        attempts++;
        gameStore.setAttempts(gameId, attempts);

        List<String> result = new ArrayList<>(Collections.nCopies(5, "absent"));
        boolean[] used = new boolean[5];

        // Mark correct letters first
        for (int i = 0; i < 5; i++) {
            if (guess.charAt(i) == answer.charAt(i)) {
                result.set(i, "correct");
                used[i] = true;
            }
        }

        // Mark present letters
        for (int i = 0; i < 5; i++) {
            if (result.get(i).equals("correct")) continue;
            for (int j = 0; j < 5; j++) {
                if (!used[j] && guess.charAt(i) == answer.charAt(j)) {
                    result.set(i, "present");
                    used[j] = true;
                    break;
                }
            }
        }

        boolean win = guess.equals(answer);
        boolean gameOver = win || attempts >= GameStore.MAX_ATTEMPTS;

        if (gameOver) {
            gameStore.removeGame(gameId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("win", win);
        response.put("gameOver", gameOver);

        // Always include answer when game ends
        if (gameOver) {
            response.put("answer", answer);
            updateRecentGame(answer, win);
        }

        return response;
    }

    public void updateRecentGame(String usedWord, boolean isWin) {
        int newStreak = 0;
        long count = recentGameRepository.count();
        Word word = wordRepository.findByWordIgnoreCase(usedWord).orElseThrow();

        Optional<RecentGame> lastGameOpt =
                recentGameRepository.findTopByOrderByUsedAtDesc();
        if (isWin) {
            newStreak = lastGameOpt
                    .map(RecentGame::getCurrentStreak)
                    .orElse(0) + 1;
        }

        if (count < MAX_RECENT_GAMES) {
            RecentGame latest = new RecentGame();
            latest.setWord(word);
            latest.setUsedAt(Instant.now());
            latest.setWin(isWin);
            latest.setCurrentStreak(newStreak);

            recentGameRepository.save(latest);
        } else {
            RecentGame oldest = recentGameRepository
                    .findFirstByOrderByUsedAtAsc()
                    .orElseThrow();

            oldest.setWord(word);
            oldest.setUsedAt(Instant.now());
            oldest.setWin(isWin);
            oldest.setCurrentStreak(newStreak);

            recentGameRepository.save(oldest);
        }
    }

    public RecentGameResp getRecentGames() {
        List<String> words = recentGameRepository.findAll().stream()
                .sorted(Comparator.comparing(RecentGame::getUsedAt).reversed())
                .map(rg ->
                        rg.getWord().getWord() + (rg.isWin() ? "1" : "0")
                )
                .toList();
        int streak = recentGameRepository.findTopByOrderByUsedAtDesc().isPresent() ?
                recentGameRepository.findTopByOrderByUsedAtDesc().get().getCurrentStreak() : 0;

        return new RecentGameResp(words, streak);
    }
}
