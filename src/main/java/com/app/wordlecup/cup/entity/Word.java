package com.app.wordlecup.cup.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "words")
@Getter
@Setter
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 5, nullable = false)
    private String word;

    @Column(name = "answer_word", nullable = true)
    private Integer answerWord = 0;

    public Word() {
    }

    public Word(Long id, String word, int answerWord) {
        this.id = id;
        this.word = word;
        this.answerWord = answerWord;
    }

    public Word(String word, int answerWord) {
        this.word = word;
        this.answerWord = answerWord;
    }

    public boolean isAnswerWord() {
        return this.answerWord == 1;
    }
}
