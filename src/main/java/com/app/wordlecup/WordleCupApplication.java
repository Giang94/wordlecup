package com.app.wordlecup;

import com.app.wordlecup.cup.repo.WordSeeder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
@EnableScheduling
public class WordleCupApplication {

    private final WordSeeder wordSeeder;

    public WordleCupApplication(WordSeeder wordSeeder) {
        this.wordSeeder = wordSeeder;
    }

    public static void main(String[] args) {
        SpringApplication.run(WordleCupApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        seedWords();
        openBrowser();
    }

    private void seedWords() {
        wordSeeder.seedIfEmpty();
    }

    private void openBrowser() {
        try {
            String url = "http://localhost:8080/index.html"; // your home page
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("Desktop not supported. Open manually: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
