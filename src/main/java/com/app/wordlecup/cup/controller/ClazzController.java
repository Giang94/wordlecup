package com.app.wordlecup.cup.controller;

import com.app.wordlecup.cup.model.*;
import com.app.wordlecup.cup.service.ClazzStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wordlecup/clazz")
public class ClazzController {

    private final ClazzStore clazzStore;

    public ClazzController(ClazzStore clazzStore) {
        this.clazzStore = clazzStore;
    }

    @PostMapping
    public ResponseEntity<Clazz> createRoom(@RequestBody CreateClazzRequest req) {
        Clazz clazz = clazzStore.createRoom(req);
        return ResponseEntity.ok(clazz);
    }

    @PostMapping("/{clazzId}/join")
    public ResponseEntity<Student> joinRoom(@PathVariable String clazzId, @RequestBody JoinClazzRequest req) {
        try {
            Student student = clazzStore.joinRoom(clazzId, req);
            return ResponseEntity.ok(student);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("room not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
            }
            throw e;
        }
    }

    @PostMapping("/{clazzId}/start")
    public ResponseEntity<Void> startClazz(@PathVariable String clazzId, @RequestBody StartClazzRequest req) {
        try {
            clazzStore.startClazz(clazzId, req);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("room not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
            }
            throw e;
        }
    }

    @PostMapping("/{clazzId}/guess")
    public ResponseEntity<StudentGuessResult> submitGuess(@PathVariable String clazzId, @RequestBody StudentGuessRequest req) {
        try {
            StudentGuessResult result = clazzStore.submitGuess(clazzId, req);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("room not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
            }
            throw e;
        }
    }

    @GetMapping("/{clazzId}")
    public ResponseEntity<Clazz> getClazz(@PathVariable String clazzId) {
        Clazz clazz = clazzStore.getClazz(clazzId);
        if (clazz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        clazzStore.finishTimedOutPlayers(clazz);
        return ResponseEntity.ok(clazz);
    }

    @GetMapping("/{clazzId}/player/{playerId}")
    public ResponseEntity<Student> getPlayerState(@PathVariable String clazzId, @PathVariable String playerId) {
        Clazz clazz = clazzStore.getClazz(clazzId);
        if (clazz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        clazzStore.finishTimedOutPlayers(clazz);
        Student student = clazzStore.getStudent(clazzId, playerId);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/{clazzId}/players")
    public ResponseEntity<?> getStudents(@PathVariable String clazzId) {
        Clazz clazz = clazzStore.getClazz(clazzId);
        if (clazz == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        clazzStore.finishTimedOutPlayers(clazz);
        return ResponseEntity.ok(new ArrayList<>(clazz.getStudents().values()));
    }

    @GetMapping("/{clazzId}/test-stats")
    public List<Map<String, Object>> getTestStats(@PathVariable String clazzId) {
        Clazz clazz = clazzStore.getClazz(clazzId);
        // Mark timed out players as finished before returning stats
        clazzStore.finishTimedOutPlayers(clazz);
        int test = clazz.getCurrentTest();
        return clazz.getStudents().values().stream()
                .map(student -> {
                    StudentTestState state = student.getTestStates().get(test);
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("playerId", student.getStudentId());
                    map.put("displayName", student.getDisplayName());
                    map.put("guessCount", state != null ? state.getGuesses().size() : 0);
                    map.put("timeTakenMillis", state != null ? state.getTimeTakenMillis() : 0);
                    map.put("testScore", state != null ? state.getTestScore() : 0);
                    map.put("totalScore", student.getTotalScore());
                    return map;
                })
                .sorted(Comparator.comparingInt(m -> (int) m.get("guessCount")))
                .collect(Collectors.toList());
    }

    @PostMapping("/{clazzId}/next-test")
    public ResponseEntity<Void> startNextTest(@PathVariable String clazzId, @RequestBody Map<String, String> req) {
        String clazzLeaderId = req.get("clazzLeaderId");
        try {
            clazzStore.startNextTest(clazzId, clazzLeaderId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/{clazzId}/restart")
    public ResponseEntity<Void> restartClazz(@PathVariable String clazzId, @RequestBody Map<String, String> req) {
        String clazzLeaderId = req.get("clazzLeaderId");
        try {
            clazzStore.restartClazz(clazzId, clazzLeaderId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}