package com.app.wordlecup.cup.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LetterResult {
    private char letter;
    private LetterResultState status;
}