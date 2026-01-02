package com.app.wordlecup.cup.model;

import lombok.Data;

@Data
public class StudentGuessRequest {

    private String clazzId;
    private String studentId;
    private String guessedWord;
}
