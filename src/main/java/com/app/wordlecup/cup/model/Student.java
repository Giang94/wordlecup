package com.app.wordlecup.cup.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class Student {
    public enum Status {WAITING, PLAYING, FINISHED}

    private final String studentId;
    private String displayName;
    private Instant joinedAt;
    private Status status = Status.WAITING;
    private int totalScore = 0;
    private final Map<Integer, StudentTestState> testStates;
}