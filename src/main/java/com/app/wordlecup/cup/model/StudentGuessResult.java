package com.app.wordlecup.cup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentGuessResult {

    private List<LetterResult> letterResults;
    private boolean win;
    private boolean finished;
}
