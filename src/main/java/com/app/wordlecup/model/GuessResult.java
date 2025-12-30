package com.app.wordlecup.model;

import java.util.List;

public record GuessResult(
        List<String> result,
        boolean win,
        boolean gameOver,
        String answer
) {
}