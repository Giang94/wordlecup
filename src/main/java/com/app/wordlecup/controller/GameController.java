package com.app.wordlecup.controller;

import com.app.wordlecup.model.GuessRequest;
import com.app.wordlecup.model.RecentGameResp;
import com.app.wordlecup.model.Word;
import com.app.wordlecup.repo.WordRepository;
import com.app.wordlecup.service.GameService;
import com.app.wordlecup.service.GameStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wordlecup")
@CrossOrigin
public class GameController {

    private final WordRepository wordRepository;
    private final GameStore gameStore;
    private final GameService gameService;


    public GameController(WordRepository wordRepository, GameStore gameStore, GameService gameService) {
        this.wordRepository = wordRepository;
        this.gameStore = gameStore;
        this.gameService = gameService;
    }

    @GetMapping("/new")
    public Map<String, String> newGame(@RequestParam String gameId) {
        Word randomWord = wordRepository.findRandomWord();
        gameStore.createGame(gameId, randomWord.getWord());
        return Map.of("gameId", gameId);
    }

    @PostMapping("/guess")
    public Map<String, Object> guess(@RequestBody GuessRequest req) {
        return gameService.guess(req.gameId(), req.guess());
    }

    @GetMapping("/recent-games")
    public RecentGameResp getRecentGames() {
        return gameService.getRecentGames();
    }
}