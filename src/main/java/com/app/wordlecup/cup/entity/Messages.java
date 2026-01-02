package com.app.wordlecup.cup.entity;

import java.util.List;
import java.util.Random;

public class Messages {

    public static final List<String> NOT_A_WORD_MESSAGES = List.of(
            "What the heck is that word?",
            "Who even says that?",
            "That’s not a word, buddy.",
            "Hmm… I don’t know that one.",
            "Where did you get that word?",
            "Are you making this up?",
            "That doesn’t exist in my dictionary.",
            "I’ve never seen that before.",
            "Is that even English?",
            "That word is… suspicious.",
            "You’re kidding me, right?"
    );

    private static final Random RANDOM = new Random();

    public static String randomNotAWordMessage() {
        return NOT_A_WORD_MESSAGES.get(RANDOM.nextInt(NOT_A_WORD_MESSAGES.size()));
    }
}