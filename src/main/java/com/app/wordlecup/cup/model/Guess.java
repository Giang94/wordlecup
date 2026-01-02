package com.app.wordlecup.cup.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Guess {
    private final String word;
    private final List<LetterResult> result;
    private final Instant timestamp;
}