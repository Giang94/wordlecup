package com.app.wordlecup.repo;

import com.app.wordlecup.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    boolean existsByWord(String word);

    @Query(value = "SELECT * FROM words w WHERE w.answer_word = 1 AND w.id NOT IN (SELECT rg.word_id FROM recent_games rg) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Word findRandomWord();

    Optional<Word> findByWordIgnoreCase(String word);
}
