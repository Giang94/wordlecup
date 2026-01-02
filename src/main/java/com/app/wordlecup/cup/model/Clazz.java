package com.app.wordlecup.cup.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class Clazz {
    public enum Status {WAITING, PLAYING, FINISHED}

    private final String clazzId;
    private final String clazzLeaderId;
    private Status status = Status.WAITING;
    private int maxStudents = 6;
    private boolean joinLocked = false;
    private int totalTests = 5;
    private int testTimeLimitSec = 120;
    private int maxAttemptsPerTest = 6;
    private int currentTest = 1;
    private Instant testStartTime;
    private Instant clazzStartTime;
    private List<String> answers;
    private final Map<String, Student> students;
    private boolean clazzEndSent = false; // Add this flag to prevent duplicate class_end events
}