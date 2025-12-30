const ROWS = 6;
const COLS = 5;

let board = [];
let row = 0;
let col = 0;
let gameId = "";

const keyState = {};

document.addEventListener("DOMContentLoaded", async () => {
  generateGameId();
  await startNewGame();
  drawBoard();
  drawKeyboard();
  listenKeys();
  setupResetButton();
  loadRecentGames();
});

function generateGameId() {
  gameId = crypto.randomUUID();
}

async function startNewGame() {
  const res = await fetch(`/api/wordlecup/new?gameId=${gameId}`);
  await res.json(); // confirm backend created game

  // Reset row and column
  row = 0;
  col = 0;

  // Reset board
  board = Array.from({ length: ROWS }, () =>
    Array.from({ length: COLS }, () => ({ letter: "", status: "" }))
  );

  // Reset key states
  Object.keys(keyState).forEach(k => delete keyState[k]);

  // Redraw
  drawBoard();
  drawKeyboard();
  clearError();
  loadRecentGames();
}

// --- Board & Keyboard ---

function drawBoard() {
  const boardDiv = document.getElementById("board");
  boardDiv.innerHTML = "";

  board.forEach((r, i) => {
    const rowDiv = document.createElement("div");
    rowDiv.className = "row";

    r.forEach((cellData, j) => {
      const cell = document.createElement("div");
      cell.className = "cell";
      cell.id = `cell-${i}-${j}`;
      cell.textContent = cellData.letter;
      if (cellData.status) cell.classList.add(cellData.status);
      rowDiv.appendChild(cell);
    });

    boardDiv.appendChild(rowDiv);
  });
}

function drawKeyboard() {
  const kb = document.getElementById("keyboard");
  kb.innerHTML = "";

  const layouts = [
    "QWERTYUIOP",
    "ASDFGHJKL",
    "ZXCVBNM"
  ];

  layouts.forEach((line, idx) => {
    const rowDiv = document.createElement("div");
    rowDiv.className = "kb-row";
    for (const ch of line) rowDiv.appendChild(createKey(ch));

    // Add special keys
    if (idx === 1) { // Row 2: DELETE
      const del = document.createElement("button");
      del.textContent = "⌫";
      del.className = "key wide";
      del.onclick = backspace;
      rowDiv.appendChild(del);
    }
    if (idx === 2) { // Row 3: ENTER
      const enter = document.createElement("button");
      enter.textContent = "ENTER";
      enter.className = "key wide";
      enter.onclick = submitGuess;
      rowDiv.appendChild(enter);
    }

    kb.appendChild(rowDiv);
  });

  // Row 4: CLEAR button
  const clearRow = document.createElement("div");
  clearRow.className = "kb-row clear-row";

  const clearBtn = document.createElement("button");
  clearBtn.textContent = "CLEAR (Space bar)";
  clearBtn.className = "key clear";
  clearBtn.onclick = clearCurrentRow;

  clearRow.appendChild(clearBtn);
  kb.appendChild(clearRow);

}

function createKey(letter) {
  const key = document.createElement("button");
  key.textContent = letter;
  key.className = "key";
  key.id = `key-${letter}`;
  key.onclick = () => handleKey(letter);
  return key;
}

function clearCurrentRow() {
  if (row >= ROWS) return;
  for (let i = 0; i < COLS; i++) board[row][i].letter = "";
  col = 0;
  drawBoard();
}

// --- Keyboard & Input ---

function listenKeys() {
  document.addEventListener("keydown", e => {
    if (/^[a-zA-Z]$/.test(e.key)) handleKey(e.key.toUpperCase());
    if (e.key === "Backspace") backspace();
    if (e.key === "Enter") submitGuess();
    if (e.key === " ") {
      clearCurrentRow();
      e.preventDefault();
    }
  });
}

function handleKey(letter) {
  if (row >= ROWS || col >= COLS) return;
  board[row][col].letter = letter;
  col++;
  drawBoard();
}

function backspace() {
  if (col === 0) return;
  col--;
  board[row][col].letter = "";
  drawBoard();
}

// --- Submit Guess ---

async function submitGuess() {
  if (col < COLS) return;
  clearError();
  const guess = board[row].map(c => c.letter).join("").toLowerCase();

  const res = await fetch("/api/wordlecup/guess", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ gameId, guess })
  });

  const data = await res.json();
  if (data.error) return showError(data.error);

  data.result.forEach((status, i) => {
    board[row][i].status = status;
    updateKey(guess[i].toUpperCase(), status);
  });

  drawBoard();

  if (data.win) {
    row = ROWS;
    showGameOver(true, data.answer);
    return;
  }

  row++;
  col = 0;

  if (data.gameOver && data.answer) showGameOver(false, data.answer);
}

function updateKey(letter, status) {
  const priority = { correct: 3, present: 2, absent: 1 };
  const current = keyState[letter];
  if (!current || priority[status] > priority[current]) {
    keyState[letter] = status;
    const keyEl = document.getElementById(`key-${letter}`);
    if (keyEl) {
      keyEl.classList.remove("correct", "present", "absent");
      keyEl.classList.add(status);
    }
  }
}

// --- Error ---

function showError(msg) {
  document.getElementById("error").textContent = msg;
}

function clearError() {
  document.getElementById("error").textContent = "";
}

// --- Reset Button ---

function setupResetButton() {
  const resetBtn = document.getElementById("reset-btn");
  if (!resetBtn) return;
  resetBtn.onclick = async () => {
    generateGameId();
    await startNewGame();
  };
}

// --- Game Over Overlay ---

function showGameOver(win, answer) {
  const overlay = document.getElementById("game-over-overlay");
  const title = document.getElementById("game-over-title");
  const answerEl = document.getElementById("game-over-answer");
  const restartBtn = document.getElementById("restart-btn");
  const searchBtn = document.getElementById("search-btn");

  title.textContent = win ? "🎉 You Win!" : "💀 Game Over";

  // Clear previous content
  answerEl.innerHTML = "";

  if (answer) {
    // Create 5 small cells
    for (const ch of answer.toUpperCase()) {
      const cell = document.createElement("div");
      cell.className = "cell correct overlay-cell"; // correct = green
      cell.textContent = ch;
      answerEl.appendChild(cell);
    }
  }

  overlay.classList.remove("hidden");

  const restart = async () => {
    overlay.classList.add("hidden");

    generateGameId();
    await startNewGame();
    drawBoard();
    drawKeyboard();

    document.removeEventListener("keydown", onKey);
  };

  const searchWord = (word) => () => {
    const query = encodeURIComponent(word);
    window.open(`https://dictionary.cambridge.org/vi/dictionary/english/${query}`, "_blank");
  };

  restartBtn.onclick = restart;
  searchBtn.onclick = searchWord(answer);

  const onKey = (e) => {
    if (e.key === "Enter") {
      restart();
    }
  };

  document.addEventListener("keydown", onKey);
}

async function loadRecentGames() {
  const res = await fetch("/api/wordlecup/recent-games");
  const resp = await res.json();
  const words = resp.recentGames;
  const streak = resp.streak;

  const list = document.getElementById("recent-games-list");
  list.innerHTML = "";

    words.forEach(w => {
      const word = w.slice(0, 5).toUpperCase();
      const result = w.slice(5); // "0" or "1"

      const li = document.createElement("li");
      li.className = "recent-game-item";

      const text = document.createElement("span");
      text.className = "recent-word";
      text.textContent = word;

      const icon = document.createElement("span");
      icon.className = "recent-result";

      if (result === "1") {
        icon.textContent = "✓";
        icon.classList.add("win");
      } else {
        icon.textContent = "✗";
        icon.classList.add("lose");
      }

      li.appendChild(text);
      li.appendChild(icon);
      list.appendChild(li);
    });

  applyUsedWordsGradient();
  updateStreak(streak);
}

function applyUsedWordsGradient() {
  const items = document.querySelectorAll("#recent-games-list li");
  if (!items.length) return;

  const base = { r: 244, g: 241, b: 234 }; // #f4f1ea
  const maxDarken = 0.45;

  const max = items.length - 1;

  items.forEach((li, i) => {
    const t = i / max;
    const factor = 1 - t * maxDarken;

    const r = Math.round(base.r * factor);
    const g = Math.round(base.g * factor);
    const b = Math.round(base.b * factor);

    li.style.backgroundColor = `rgb(${r}, ${g}, ${b})`;
  });
}

function updateStreak(streak) {
  const streakEl = document.querySelector(".streak");
  streakEl.textContent = `🔥Streak: ${streak}🔥`;
}