package com.app.wordlecup.cup.service;

import com.app.wordlecup.cup.model.Clazz;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClazzTimeoutScheduler {

    private final ClazzStore clazzStore;

    public ClazzTimeoutScheduler(ClazzStore clazzStore) {
        this.clazzStore = clazzStore;
    }

    // Run every second
    @Scheduled(fixedRate = 1000)
    public void checkTimeouts() {
        Map<String, Clazz> rooms = clazzStore.getAllClazzes();
        for (Clazz clazz : rooms.values()) {
            clazzStore.finishTimedOutPlayers(clazz);
        }
    }
}

