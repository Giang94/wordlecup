package com.app.wordlecup.model;

import jakarta.persistence.*;

@Entity
@Table(name = "words")
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

    public Word( String word, int answerWord) {
        this.word = word;
        this.answerWord = answerWord;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getAnswerWord() {
        return answerWord;
    }

    public void setAnswerWord(int answerWord) {
        this.answerWord = answerWord;
    }

    public boolean isAnswerWord() {
        return this.answerWord == 1;
    }
}
