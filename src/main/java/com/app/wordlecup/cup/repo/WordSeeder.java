package com.app.wordlecup.cup.repo;

import com.app.wordlecup.cup.entity.RecentClazz;
import com.app.wordlecup.cup.entity.Word;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WordSeeder {

    private final WordRepository wordRepository;
    private final RecentClazzRepository recentClazzRepository;

    public WordSeeder(WordRepository wordRepository, RecentClazzRepository recentClazzRepository) {
        this.wordRepository = wordRepository;
        this.recentClazzRepository = recentClazzRepository;
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

    public void seedRecentClazzs() {
        for (int i = 0; i < RecentClazz.MAX_RECENT_GAMES; i++) {
            Word answerWords = wordRepository.findRandomWord();
            boolean isWin = new Random().nextBoolean();

            var recentClazz = new RecentClazz();
            recentClazz.setWord(answerWords);
            recentClazz.setWin(isWin);
            recentClazzRepository.save(recentClazz);
            System.out.println("Seeded recent class with word: " + answerWords.getWord() + ", isWin: " + isWin);
        }
    }

    @Transactional
    public void rebuildStreaks() {
        List<RecentClazz> clazzes = recentClazzRepository.findAllByOrderByUsedAtAsc();

        int streak = 0;

        for (RecentClazz clazz : clazzes) {
            if (clazz.isWin()) {
                streak++;
            } else {
                streak = 0;
            }

            clazz.setCurrentStreak(streak);
        }

        recentClazzRepository.saveAll(clazzes);
    }
}
