package com.app.wordlecup.cup.service;

import com.app.wordlecup.cup.model.LetterResult;

import java.util.ArrayList;
import java.util.List;

import static com.app.wordlecup.cup.model.LetterResultState.*;

public class WordleEngine {

    public static List<LetterResult> evaluate(String answer, String guess) {
        answer = answer.toUpperCase();
        guess = guess.toUpperCase();
        int len = answer.length();
        List<LetterResult> results = new ArrayList<>();
        boolean[] answerUsed = new boolean[len];

        // First pass: check for correct letters
        for (int i = 0; i < len; i++) {
            char g = guess.charAt(i);
            if (g == answer.charAt(i)) {
                results.add(new LetterResult(g, CORRECT));
                answerUsed[i] = true;
            } else {
                results.add(null); // placeholder
            }
        }

        // Second pass: check for present letters
        for (int i = 0; i < len; i++) {
            if (results.get(i) != null) continue; // already correct
            char g = guess.charAt(i);
            boolean found = false;
            for (int j = 0; j < len; j++) {
                if (!answerUsed[j] && g == answer.charAt(j)) {
                    found = true;
                    answerUsed[j] = true;
                    break;
                }
            }
            if (found) {
                results.set(i, new LetterResult(g, PRESENT));
            } else {
                results.set(i, new LetterResult(g, ABSENT));
            }
        }
        return results;
    }
}
