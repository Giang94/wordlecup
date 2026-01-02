package com.app.wordlecup.cup.service;

import com.app.wordlecup.cup.config.ClazzWebSocketHandler;
import com.app.wordlecup.cup.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClazzStore {

    private static final String ROOM_ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 6;
    private static final Random RANDOM = new Random();
    private final Map<String, Clazz> clazzes = new ConcurrentHashMap<>();

    private String generateRoomId() {
        StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            int idx = (int) (Math.random() * ROOM_ID_CHARS.length());
            sb.append(ROOM_ID_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    public Clazz createRoom(CreateClazzRequest req) {
        String clazzId = generateRoomId();
        Clazz clazz = new Clazz(
                clazzId,
                req.getClazzLeaderId(),
                Clazz.Status.WAITING,
                req.getMaxStudents(),
                false,
                req.getTotalTests(),
                req.getTestTimeLimitSec(),
                req.getMaxAttemptsPerTest(),
                1,
                null, // testStartTime
                null, // clazzStartTime
                new ArrayList<>(),
                new ConcurrentHashMap<>(),
                false
        );
        // Add clazzLeader as first player
        Student clazzLeader = new Student(
                req.getClazzLeaderId(),
                req.getClazzLeaderDisplayName(),
                Instant.now(),
                Student.Status.WAITING,
                0,
                new ConcurrentHashMap<>()
        );
        clazz.getStudents().put(clazzLeader.getStudentId(), clazzLeader);
        clazzes.put(clazzId, clazz);

        System.out.println("Created room: " + clazzId + " by clazzLeader: " + req.getClazzLeaderId());
        return clazz;
    }

    public Student joinRoom(String clazzId, JoinClazzRequest req) {
        Clazz clazz = getRoomOrThrow(clazzId);
        if (clazz.isJoinLocked() || clazz.getStatus() != Clazz.Status.WAITING) {
            throw new IllegalArgumentException("Room is not open for joining");
        }
        if (clazz.getStudents().size() >= clazz.getMaxStudents()) {
            throw new IllegalArgumentException("Room is full");
        }
        if (clazz.getStudents().containsKey(req.getStudentId())) {
            // Player already joined, return existing player
            return clazz.getStudents().get(req.getStudentId());
        }
        String displayName = req.getDisplayName() != null && !req.getDisplayName().isEmpty()
                ? req.getDisplayName() : req.getStudentId();
        Student student = new Student(
                req.getStudentId(),
                displayName,
                Instant.now(),
                Student.Status.WAITING,
                0,
                new ConcurrentHashMap<>()
        );
        clazz.getStudents().put(student.getStudentId(), student);

        System.out.println("Player " + student.getStudentId() + " joined room: " + clazzId);
        return student;
    }

    public void startClazz(String clazzId, StartClazzRequest req) {
        Clazz clazz = getRoomOrThrow(clazzId);
        if (!clazz.getClazzLeaderId().equals(req.getClazzLeaderId())) {
            throw new IllegalArgumentException("Only class leader can start the class");
        }
        if (clazz.getStatus() != Clazz.Status.WAITING) {
            throw new IllegalArgumentException("Clazz already started");
        }
        // Generate answers for each test
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < clazz.getTotalTests(); i++) {
            answers.add("APPLE"); // Replace with real word selection
        }
        clazz.setAnswers(answers);
        clazz.setStatus(Clazz.Status.PLAYING);
        clazz.setJoinLocked(true);
        clazz.setCurrentTest(1);
        Instant startTime = Instant.now().plusSeconds(3); // 3 seconds countdown
        clazz.setClazzStartTime(startTime);
        clazz.setTestStartTime(startTime);
        clazz.setClazzEndSent(false); // Reset flag
        // Initialize PlayerTestState for each player
        for (Student student : clazz.getStudents().values()) {
            student.setStatus(Student.Status.PLAYING);
            student.getTestStates().put(1, new StudentTestState(1));
        }
        System.out.println("Clazz started in room: " + clazzId + " by class leader: " + req.getClazzLeaderId() + " (start at " + startTime + ")");
        ClazzWebSocketHandler.broadcastToRoom(clazzId, "{\"event\":\"update\",\"clazzId\":\"" + clazzId + "\"}");
    }

    public StudentGuessResult submitGuess(String clazzId, StudentGuessRequest req) {
        Clazz clazz = getRoomOrThrow(clazzId);
        Student student = getStudent(clazzId, req.getStudentId());
        int test = clazz.getCurrentTest();
        StudentTestState testState = student.getTestStates().get(test);
        if (testState == null || testState.isFinished()) {
            throw new IllegalArgumentException("Test not active or already finished");
        }
        String answer = clazz.getAnswers().get(test - 1);
        List<LetterResult> letterResults = WordleEngine.evaluate(answer, req.getGuessedWord());
        Guess guess = new Guess(req.getGuessedWord(), letterResults, Instant.now());
        testState.getGuesses().add(guess);
        testState.setAttemptsUsed(testState.getAttemptsUsed() + 1);
        if (testState.getFirstGuessAt() == null) {
            testState.setFirstGuessAt(Instant.now());
        }
        boolean win = req.getGuessedWord().equalsIgnoreCase(answer);
        if (win || testState.getAttemptsUsed() >= clazz.getMaxAttemptsPerTest()) {
            testState.setFinished(true);
            testState.setFinishedAt(Instant.now());
            testState.setWin(win);
            student.setStatus(Student.Status.FINISHED);
            if (testState.getFirstGuessAt() != null && testState.getFinishedAt() != null) {
                long timeTaken = java.time.Duration.between(testState.getFirstGuessAt(), testState.getFinishedAt()).toMillis();
                testState.setTimeTakenMillis(timeTaken);
            }
            // Assign a random testScore (10, 20, ..., 100)
            int randomScore = (RANDOM.nextInt(10) + 1) * 10;
            testState.setTestScore(randomScore);
            student.setTotalScore(student.getTotalScore() + randomScore);
        }
        // Scoring logic can be added here
        StudentGuessResult result = new StudentGuessResult(letterResults, win, testState.isFinished());
        ClazzWebSocketHandler.broadcastToRoom(clazzId, "{\"event\":\"update\",\"clazzId\":\"" + clazzId + "\"}");
        // Check if class should end after this guess
        boolean allFinished = clazz.getStudents().values().stream()
                .map(p -> p.getTestStates().get(test))
                .allMatch(state -> state != null && state.isFinished());
        if (allFinished && test == clazz.getTotalTests() && !clazz.isClazzEndSent()) {
            clazz.setStatus(Clazz.Status.FINISHED);
            clazz.setJoinLocked(true);
            clazz.setClazzEndSent(true);
            ClazzWebSocketHandler.broadcastToRoom(clazzId, "{\"event\":\"class_end\",\"clazzId\":\"" + clazzId + "\"}");
        }
        return result;
    }

    public void startNextTest(String clazzId, String clazzLeaderId) {
        Clazz clazz = getRoomOrThrow(clazzId);
        if (!clazz.getClazzLeaderId().equals(clazzLeaderId)) {
            throw new IllegalArgumentException("Only class leader can start next test");
        }
        if (clazz.getCurrentTest() >= clazz.getTotalTests()) {
            throw new IllegalArgumentException("No more tests");
        }
        int nextTest = clazz.getCurrentTest() + 1;
        clazz.setCurrentTest(nextTest);
        Instant startTime = Instant.now().plusSeconds(3); // 3s countdown
        clazz.setTestStartTime(startTime);
        // Optionally: set new answer for this test if needed
        // room.getAnswers().set(nextTest - 1, ...);
        for (Student student : clazz.getStudents().values()) {
            student.setStatus(Student.Status.PLAYING);
            student.getTestStates().put(nextTest, new StudentTestState(nextTest));
        }
        System.out.println("Next test started: " + nextTest + " in room: " + clazzId);
        ClazzWebSocketHandler.broadcastToRoom(clazzId, "{\"event\":\"update\",\"clazzId\":\"" + clazzId + "\"}");
    }

    public void restartClazz(String clazzId, String clazzLeaderId) {
        Clazz clazz = getRoomOrThrow(clazzId);
        if (!clazz.getClazzLeaderId().equals(clazzLeaderId)) {
            throw new IllegalArgumentException("Only class leader can restart the class");
        }
        // Reset room state
        clazz.setStatus(Clazz.Status.PLAYING);
        clazz.setJoinLocked(false);
        clazz.setCurrentTest(1);
        clazz.setClazzEndSent(false); // Reset flag
        // Generate new answers for each test
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < clazz.getTotalTests(); i++) {
            answers.add("APPLE"); // Replace with real word selection
        }
        clazz.setAnswers(answers);
        clazz.setClazzStartTime(Instant.now());
        clazz.setTestStartTime(Instant.now());
        // Reset all players
        for (Student student : clazz.getStudents().values()) {
            student.setStatus(Student.Status.PLAYING);
            student.setTotalScore(0);
            student.getTestStates().clear();
            student.getTestStates().put(1, new StudentTestState(1));
        }
        System.out.println("Room " + clazzId + " restarted by class leader: " + clazzLeaderId);
        ClazzWebSocketHandler.broadcastToRoom(clazzId, "{\"event\":\"update\",\"clazzId\":\"" + clazzId + "\"}");
    }

    public Clazz getClazz(String clazzId) {
        return clazzes.get(clazzId);
    }

    public Student getStudent(String clazzId, String playerId) {
        Clazz clazz = getRoomOrThrow(clazzId);
        Student student = clazz.getStudents().get(playerId);
        if (student == null) throw new IllegalArgumentException("Player not found");
        return student;
    }

    public void removeRoom(String clazzId) {
        clazzes.remove(clazzId);
    }

    public Map<String, Clazz> getAllClazzes() {
        return clazzes;
    }

    // --- Helpers ---
    private Clazz getRoomOrThrow(String clazzId) {
        System.out.println("Fetching class: " + clazzId);
        Clazz clazz = clazzes.get(clazzId);
        if (clazz == null) throw new IllegalArgumentException("Room not found");
        return clazz;
    }

    /**
     * Mark all unfinished players as finished if test time is up.
     */
    public void finishTimedOutPlayers(Clazz clazz) {
        Instant now = Instant.now();
        int test = clazz.getCurrentTest();
        Instant testStart = clazz.getTestStartTime();
        long testTimeLimitMillis = clazz.getTestTimeLimitSec() * 1000L;
        if (testStart == null) return;
        if (now.isBefore(testStart.plusMillis(testTimeLimitMillis))) return; // test still active
        boolean anyFinished = false;
        for (Student student : clazz.getStudents().values()) {
            StudentTestState state = student.getTestStates().get(test);
            if (state != null && !state.isFinished()) {
                state.setFinished(true);
                state.setFinishedAt(testStart.plusMillis(testTimeLimitMillis));
                state.setWin(false);
                student.setStatus(Student.Status.FINISHED);
                if (state.getFirstGuessAt() == null) {
                    state.setFirstGuessAt(testStart);
                }
                long timeTaken = java.time.Duration.between(state.getFirstGuessAt(), state.getFinishedAt()).toMillis();
                state.setTimeTakenMillis(timeTaken);
                // Assign a random testScore (10, 20, ..., 100)
                int randomScore = (RANDOM.nextInt(10) + 1) * 10;
                state.setTestScore(randomScore);
                student.setTotalScore(student.getTotalScore() + randomScore);
                anyFinished = true;
            }
        }
        if (anyFinished) {
            ClazzWebSocketHandler.broadcastToRoom(clazz.getClazzId(), "{\"event\":\"update\",\"clazzId\":\"" + clazz.getClazzId() + "\"}");
        }
        // Check if clazz should end
        boolean allFinished = clazz.getStudents().values().stream()
                .map(p -> p.getTestStates().get(test))
                .allMatch(state -> state != null && state.isFinished());
        if (allFinished && test == clazz.getTotalTests() && !clazz.isClazzEndSent()) {
            clazz.setStatus(Clazz.Status.FINISHED);
            clazz.setJoinLocked(true);
            clazz.setClazzEndSent(true);
            ClazzWebSocketHandler.broadcastToRoom(clazz.getClazzId(), "{\"event\":\"class_end\",\"clazzId\":\"" + clazz.getClazzId() + "\"}");
        }
    }
}