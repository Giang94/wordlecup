package com.app.wordlecup.cup.model;

import lombok.Data;

@Data
public class CreateClazzRequest {
    private String clazzLeaderId;
    private String clazzLeaderDisplayName;
    private int maxStudents = 6;
    private int totalTests = 5;
    private int testTimeLimitSec = 120;
    private int maxAttemptsPerTest = 6;
}
