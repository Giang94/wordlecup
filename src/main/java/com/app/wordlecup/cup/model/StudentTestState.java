package com.app.wordlecup.cup.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class StudentTestState {

    public StudentTestState(int testNumber) {
        this.testNumber = testNumber;
    }

    private final int testNumber;
    private final List<Guess> guesses = new ArrayList<>();
    private int attemptsUsed = 0;
    private boolean win = false;
    private boolean finished = false;
    private Instant firstGuessAt;
    private Instant finishedAt;
    private long timeTakenMillis = 0;
    private int testScore = 0;

    public void finishTest() {
        this.finished = true;
        this.timeTakenMillis = Duration.between(firstGuessAt, finishedAt).toMillis();
    }
}
