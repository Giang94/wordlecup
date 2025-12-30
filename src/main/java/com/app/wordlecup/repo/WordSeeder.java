package com.app.wordlecup.repo;

import com.app.wordlecup.model.RecentGame;
import com.app.wordlecup.model.Word;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WordSeeder {

    private final WordRepository wordRepository;
    private final RecentGameRepository recentGameRepository;

    public WordSeeder(WordRepository wordRepository, RecentGameRepository recentGameRepository) {
        this.wordRepository = wordRepository;
        this.recentGameRepository = recentGameRepository;
    }

    public void seedIfEmpty() {
        long count = wordRepository.count();

        if (count > 0) {
            System.out.println("WORDS table already has data (" + count + "). Skipping seeding 🌱");
            return;
        }

        System.out.println("WORDS table empty. Seeding words 🌊");
        loadWords("5-words-all.txt");
        loadAnswerWords("5-words-answer.txt");
    }

    public void loadWords(String resourceFileName) {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {

            if (is == null) {
                System.out.println(resourceFileName + " not found in resources!");
                return;
            }

            try (var reader = new BufferedReader(new InputStreamReader(is))) {

                // Read, filter, and map
                List<Word> words = reader.lines()
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(w -> w.matches("^[a-z]{5}$")) // only 5-letter alphabetic
                        .collect(Collectors.toCollection(LinkedHashSet::new)) // remove duplicates
                        .stream()
                        .map(w -> new Word(null, w, 0))
                        .collect(Collectors.toList());

                // Save to DB
                wordRepository.saveAll(words);

                System.out.println("Loaded " + words.size() + " words into DB.");
            }

        } catch (Exception e) {
            System.err.println("Error loading words: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadAnswerWords(String resourceFileName) {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {

            if (is == null) {
                System.out.println(resourceFileName + " not found in resources!");
                return;
            }
            System.out.println("-------------------------------");
            try (var reader = new BufferedReader(new InputStreamReader(is))) {
                Set<String> wordsToLoad = reader.lines()
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .filter(w -> w.matches("^[a-z]{5}$"))
                        .collect(Collectors.toSet()); // remove duplicates

                int updatedCount = 0;
                int insertedCount = 0;

                for (String wordStr : wordsToLoad) {
                    Optional<Word> existing = wordRepository.findByWordIgnoreCase(wordStr);
                    if (existing.isPresent()) {
                        Word w = existing.get();
                        if (!w.isAnswerWord()) {
                            w.setAnswerWord(1);
                            wordRepository.save(w);
                            updatedCount++;
                        }
                    } else {
                        System.out.println("Inserting new answer word: " + wordStr);
                        Word w = new Word(wordStr, 1);
                        wordRepository.save(w);
                        insertedCount++;
                    }
                }


                System.out.println("Updated " + updatedCount + " words to answerWord=1.");
                System.out.println("Inserted " + insertedCount + " new answer words.");
                System.out.println("Total answer words loaded: " + wordsToLoad.size());
                System.out.println("-------------------------------");
            }

        } catch (Exception e) {
            System.err.println("Error loading answer words: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void seedRecentGames() {
        for (int i = 0; i < RecentGame.MAX_RECENT_GAMES; i++) {
            Word answerWords = wordRepository.findRandomWord();
            boolean isWin = new Random().nextBoolean();

            var recentGame = new com.app.wordlecup.model.RecentGame();
            recentGame.setWord(answerWords);
            recentGame.setWin(isWin);
            recentGameRepository.save(recentGame);
            System.out.println("Seeded recent game with word: " + answerWords.getWord() + ", isWin: " + isWin);
        }
    }

    @Transactional
    public void rebuildStreaks() {
        List<RecentGame> games = recentGameRepository.findAllByOrderByUsedAtAsc();

        int streak = 0;

        for (RecentGame game : games) {
            if (game.isWin()) {
                streak++;
            } else {
                streak = 0;
            }

            game.setCurrentStreak(streak);
        }

        recentGameRepository.saveAll(games);
    }
}
