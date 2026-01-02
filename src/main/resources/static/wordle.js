// Multistudent gameplay.js based on single/script.js
// Assumes clazzId and username are passed in URL

const ROWS = 6;
const COLS = 5;

let board = [];
let row = 0;
let col = 0;
let clazzId = null;
let username = null;
const keyState = {};
let students = [];
let gameStartTime = null;
let countdownActive = false;
let localStudentFinished = false;
let localStudentWon = false;
let localGuessCount = 0;
let testTimeLimitSec = 120;
let testCountdownInterval = null;
let statsPopupCountdown = null;
let statsPopupShown = false;
let lastKnownTest = 1;
let ws;

// Parse clazzId and username from URL
(() => {
    const params = new URLSearchParams(window.location.search);
    clazzId = params.get('clazzId');
    username = params.get('username');
    if (!clazzId || !username) {
        document.body.innerHTML = '<h2>Missing room or username.</h2>';
        throw new Error('Missing room or username');
    }
})();

document.addEventListener("DOMContentLoaded", async () => {
    // await joinGame();
    await fetchGameState();
    await fetchStudents();
    drawBoard();
    drawKeyboard();
    listenKeys();
    connectWebSocket();
});

function connectWebSocket() {
    ws = new WebSocket(`ws://${window.location.host}/ws/clazz?clazzId=${clazzId}`);
    ws.onmessage = async function(event) {
        const data = JSON.parse(event.data);
        if (data.event === 'update') {
            await fetchGameState();
            await fetchStudents();
            if (!statsPopupShown) await maybeShowStatsPopup();
        }
        if (data.event === 'game_end') {
            await handleGameEnd();
        }
    };
    ws.onclose = function() {
        setTimeout(connectWebSocket, 2000);
    };
}

function resetForNewTest(room) {
    board = Array.from({ length: ROWS }, () =>
        Array.from({ length: COLS }, () => ({ letter: "", status: "" }))
    );
    row = 0;
    col = 0;
    localGuessCount = 0;
    localStudentFinished = false;
    localStudentWon = false;
    for (const k in keyState) delete keyState[k];
    drawBoard();
    drawKeyboard();
    clearError();
    // Optionally, update UI to indicate new test
}

function updateTestLabel(current, total) {
    let label = document.getElementById('test-label');
    if (!label) {
        label = document.createElement('div');
        label.id = 'test-label';
        label.style.fontSize = '1.3em';
        label.style.fontWeight = 'bold';
        label.style.margin = '10px 0 10px 0';
        // Insert at top of center panel if exists, else at top of body
        const center = document.getElementById('center-panel');
        if (center) center.insertBefore(label, center.firstChild);
        else document.body.insertBefore(label, document.body.firstChild);
    }
    label.textContent = `Test ${current}/${total}`;
}

async function fetchGameState() {
    // Fetch current board state for this student (implement backend as needed)
    // Fetch room info to get gameStartTime
    const res = await fetch(`/wordlecup/clazz/${clazzId}`);
    if (res.status === 404) {
        window.location.href = '/index.html';
        return;
    }
    const room = await res.json();
    window.lastRoomStatus = room.status;

    // Update test label
    updateTestLabel(room.currentTest, room.totalTests);

    // Set current test globally for allStudentsFinished()
    window.currentTest = room.currentTest;
    window.roomTotalRounds = room.totalRounds;
    if (room.status === 'FINISHED' && !statsPopupShown) {
        handleGameEnd();
    }

    // Detect new test
    if (room.currentRound !== lastKnownRound) {
        resetForNewRound(room);
        lastKnownRound = room.currentRound;
    }

    if (room.gameStartTime) {
        gameStartTime = new Date(room.gameStartTime).getTime();
        maybeShowCountdown();
    }

    // Start test timer
    startRoundCountdown(room);
}

function maybeShowCountdown() {
    if (!gameStartTime) return;
    const now = Date.now();
    const diff = Math.floor((gameStartTime - now) / 1000);
    if (diff > 0) {
        showCountdown(diff);
    } else {
        hideCountdown();
    }
}

function showCountdown(seconds) {
    countdownActive = true;
    const overlay = document.getElementById('countdown-overlay');
    const timer = document.getElementById('countdown-timer');
    overlay.classList.remove('hidden');
    timer.textContent = seconds;
    let remaining = seconds;
    const interval = setInterval(() => {
        remaining--;
        if (remaining > 0) {
            timer.textContent = remaining;
        } else {
            clearInterval(interval);
            hideCountdown();
        }
    }, 1000);
}

function hideCountdown() {
    countdownActive = false;
    const overlay = document.getElementById('countdown-overlay');
    overlay.classList.add('hidden');
}

async function fetchStudents() {
    // Fetch the list of students in the room
    try {
        const res = await fetch(`/wordlecup/clazz/${clazzId}/students`);
        if (res.status === 404) {
            window.location.href = '/index.html';
            return;
        }
        students = await res.json();
        renderStudentList();
    } catch (e) {
        // Optionally handle error
    }
}

function renderStudentList() {
    const ul = document.getElementById('student-list');
    ul.innerHTML = '';
    students.forEach(p => {
        let guessCount = 0;
        let win = false;
        let finished = false;
        if (p.studentId === username) {
            guessCount = localGuessCount;
            // Use backend for win/finished icons for consistency
            if (p.roundStates && p.roundStates[1]) {
                finished = !!p.roundStates[1].finished;
                win = !!p.roundStates[1].win;
            }
        } else if (p.roundStates && p.roundStates[1] && Array.isArray(p.roundStates[1].guesses)) {
            guessCount = p.roundStates[1].guesses.length;
            finished = !!p.roundStates[1].finished;
            win = !!p.roundStates[1].win;
        }
        if (guessCount > 6) guessCount = 6;
        const li = document.createElement('li');
        li.textContent = `${p.displayName || p.studentId || 'Unknown'} (${guessCount}/6)`;
        if (p.isHost || p.owner || p.host) {
            li.textContent += ' (host)';
        }
        // Add win/lose icon
        if (finished) {
            if (win) {
                const tick = document.createElement('span');
                tick.textContent = ' ✔';
                tick.style.color = 'green';
                tick.style.fontWeight = 'bold';
                li.appendChild(tick);
            } else if (guessCount === 6) {
                const cross = document.createElement('span');
                cross.textContent = ' ✖';
                cross.style.color = 'red';
                cross.style.fontWeight = 'bold';
                li.appendChild(cross);
            }
        }
        if (p.status && p.status.toLowerCase() === 'finished') {
            li.style.opacity = 0.5;
        }
        ul.appendChild(li);
    });
}

function drawBoard() {
    const boardDiv = document.getElementById("board");
    boardDiv.innerHTML = "";
    if (!board.length) {
        board = Array.from({ length: ROWS }, () =>
            Array.from({ length: COLS }, () => ({ letter: "", status: "" }))
        );
    }
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
    const layouts = ["QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"];
    layouts.forEach((line, idx) => {
        const rowDiv = document.createElement("div");
        rowDiv.className = "kb-row";
        for (const ch of line) rowDiv.appendChild(createKey(ch));
        if (idx === 1) {
            const del = document.createElement("button");
            del.textContent = "⌫";
            del.className = "key wide";
            del.onclick = backspace;
            rowDiv.appendChild(del);
        }
        if (idx === 2) {
            const enter = document.createElement("button");
            enter.textContent = "ENTER";
            enter.className = "key wide";
            enter.onclick = submitGuess;
            rowDiv.appendChild(enter);
        }
        kb.appendChild(rowDiv);
    });
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

function listenKeys() {
    document.addEventListener("keydown", e => {
        if (countdownActive) return;
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
    if (row >= ROWS || col >= COLS || localStudentFinished || localStudentWon) return;
    board[row][col].letter = letter;
    col++;
    drawBoard();
}

function backspace() {
    if (col === 0 || localStudentFinished || localStudentWon) return;
    col--;
    board[row][col].letter = "";
    drawBoard();
}

async function submitGuess() {
    if (col < COLS || localStudentFinished || localStudentWon) return;
    clearError();
    const guess = board[row].map(c => c.letter).join("").toLowerCase();
    const res = await fetch(`/wordlecup/clazz/${clazzId}/guess`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ studentId: username, word: guess })
    });
    if (res.status === 404) {
        window.location.href = '/index.html';
        return;
    }
    const data = await res.json();
    if (data.error) return showError(data.error);
    data.letterResults.forEach((status, i) => {
        board[row][i].status = status.status.toLowerCase();
        updateKey(guess[i].toUpperCase(), status.status.toLowerCase());
    });
    drawBoard();
    localGuessCount++;
    if (data.win || (data.letterResults && data.letterResults.every(lr => lr.status && lr.status.toLowerCase() === 'correct'))) {
        row = ROWS;
        localStudentWon = true;
        localStudentFinished = true;
        renderStudentList(); // update instantly
        showError('🎉 You Got It! 🎉');
        return;
    }
    row++;
    col = 0;
    renderStudentList(); // update instantly
    if (data.gameOver && data.answer) {
        localStudentFinished = true;
        showClassRoomOver(false, data.answer);
    }
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

function showError(msg) {
    document.getElementById("error").textContent = msg;
}

function clearError() {
    document.getElementById("error").textContent = "";
}

function showClassRoomOver(win, answer) {
    if (win) {
        // Only show message, not popup, for win
        showError('🎉 You Win!');
        return;
    }
    const overlay = document.getElementById("class-room-over-overlay");
    const title = document.getElementById("class-room-over-title");
    const answerEl = document.getElementById("class-room-over-answer");
    const restartBtn = document.getElementById("restart-btn");
    const searchBtn = document.getElementById("search-btn");
    title.textContent = "💀 Class Over";
    answerEl.innerHTML = "";
    if (answer) {
        for (const ch of answer.toUpperCase()) {
            const cell = document.createElement("div");
            cell.className = "cell correct overlay-cell";
            cell.textContent = ch;
            answerEl.appendChild(cell);
        }
    }
    overlay.classList.remove("hidden");
    const restart = async () => {
        overlay.classList.add("hidden");
        await fetchGameState();
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

// Add HTML for stats popup
document.addEventListener("DOMContentLoaded", () => {
    if (!document.getElementById('stats-overlay')) {
        const statsOverlay = document.createElement('div');
        statsOverlay.id = 'stats-overlay';
        statsOverlay.className = 'overlay hidden';
        statsOverlay.innerHTML = `
            <div class="overlay-content">
                <h2>Test Stats</h2>
                <table id="stats-table" style="width:100%;margin-bottom:16px;"></table>
            </div>
        `;
        document.body.appendChild(statsOverlay);
    }
});

function allStudentsFinished() {
    if (!students || students.length === 0) return false;
    const test = window.currentRound || 1;
    return students.every(p => p.roundStates && p.roundStates[test] && p.roundStates[test].finished);
}

async function maybeShowStatsPopup() {
    if (statsPopupShown) return;
    // Re-fetch students if not all finished
    if (!allStudentsFinished()) {
        await fetchStudents();
        if (!allStudentsFinished()) return;
    }
    // Fetch stats and answer from backend
    const res = await fetch(`/wordlecup/clazz/${clazzId}/test-stats`);
    if (!res.ok) return;
    const stats = await res.json();
    // Fetch the answer for this test
    let answer = '';
    try {
        const roomRes = await fetch(`/wordlecup/clazz/${clazzId}`);
        if (roomRes.ok) {
            const room = await roomRes.json();
            if (room.answers && Array.isArray(room.answers) && room.currentRound) {
                answer = room.answers[room.currentRound - 1];
            }
        }
    } catch (e) {}
    await showStatsPopup(stats, answer);
}

async function showStatsPopup(stats, answer) {
    console.log("showStatsPopup() called");
    statsPopupShown = true;
    const overlay = document.getElementById('stats-overlay');
    const table = document.getElementById('stats-table');
    // Show answer above the table
    let answerDiv = document.getElementById('stats-answer');
    if (!answerDiv) {
        answerDiv = document.createElement('div');
        answerDiv.id = 'stats-answer';
        answerDiv.style.fontSize = '1.1em';
        answerDiv.style.fontWeight = 'bold';
        answerDiv.style.margin = '0 0 10px 0';
        table.parentNode.insertBefore(answerDiv, table);
    }
    if (answer) {
        answerDiv.innerHTML = `Answer: <span style="letter-spacing:2px;font-family:monospace;">${answer.toUpperCase()}</span>`;
    } else {
        answerDiv.innerHTML = '';
    }
    table.innerHTML = `<tr><th>Student</th><th>Guesses</th><th>Time (s)</th><th>Test Score</th><th>Total Score</th></tr>` +
        stats.map(row => {
            const timeSec = (row.timeTakenMillis / 1000).toFixed(2);
            return `<tr><td>${row.displayName || row.studentId}</td><td>${row.guessCount}/6</td><td>${timeSec}</td><td>${row.roundScore}</td><td>${row.totalScore}</td></tr>`;
        }).join('');
    let msgDiv = document.getElementById('stats-popup-msg');
    if (!msgDiv) {
        msgDiv = document.createElement('div');
        msgDiv.id = 'stats-popup-msg';
        msgDiv.style.fontSize = '1.2em';
        msgDiv.style.margin = '12px 0 0 0';
        msgDiv.style.fontWeight = 'bold';
        table.parentNode.insertBefore(msgDiv, table.nextSibling);
    }

        let countdown = 3;
        msgDiv.textContent = `Next test starts in ${countdown} seconds...`;
        if (statsPopupCountdown) clearInterval(statsPopupCountdown);
        statsPopupCountdown = setInterval(async () => {
            countdown--;
            if (countdown > 0) {
                msgDiv.textContent = `Next test starts in ${countdown} seconds...`;
            } else {
                clearInterval(statsPopupCountdown);
                statsPopupCountdown = null;
                overlay.classList.add('hidden');
                const timerDiv = document.getElementById('test-timer');
                if (timerDiv) timerDiv.style.display = '';
                statsPopupShown = false;
                await maybeStartNextRound();
                await fetchGameState();
                await fetchStudents();
            }
        }, 1000);

    overlay.classList.remove('hidden');
    const timerDiv = document.getElementById('test-timer');
    if (timerDiv) timerDiv.style.display = 'none';
    if (roundCountdownInterval) {
        clearInterval(roundCountdownInterval);
        roundCountdownInterval = null;
    }
}

// Start next test if host and more rounds remain
async function maybeStartNextRound() {
    // Fetch room info to check test and host
    const res = await fetch(`/wordlecup/clazz/${clazzId}`);
    if (!res.ok) return;
    const room = await res.json();
    // Only host can start next test
    if (room.clazzLeaderId === username && room.currentRound < room.totalRounds) {
        // Call backend to start next test
        await fetch(`/wordlecup/clazz/${clazzId}/next-test`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ clazzLeaderId: username })
        });
    }
}

function updateRoundTimerUI(secondsLeft) {
    let timerDiv = document.getElementById('test-timer');
    if (!timerDiv) {
        timerDiv = document.createElement('div');
        timerDiv.id = 'test-timer';
        timerDiv.style.fontSize = '2em';
        timerDiv.style.fontWeight = 'bold';
        timerDiv.style.margin = '12px 0';
        timerDiv.style.letterSpacing = '2px';
        // Insert at top of center panel if exists, else at top of body
        const center = document.getElementById('center-panel');
        if (center) center.insertBefore(timerDiv, center.firstChild);
        else document.body.insertBefore(timerDiv, document.body.firstChild);
    }
    const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0');
    const ss = String(secondsLeft % 60).padStart(2, '0');
    timerDiv.textContent = `${mm}:${ss}`;
    if (secondsLeft <= 0) {
        timerDiv.style.color = 'red';
        timerDiv.textContent = '00:00';
    } else {
        timerDiv.style.color = '';
    }
}

function startRoundCountdown(room) {
    if (roundCountdownInterval) clearInterval(roundCountdownInterval);
    if (!room || !room.roundStartTime || !room.roundTimeLimitSec) return;
    roundTimeLimitSec = room.roundTimeLimitSec;
    const roundStart = new Date(room.roundStartTime).getTime();
    const now = Date.now();
    let secondsLeft = Math.max(0, Math.floor((roundStart + roundTimeLimitSec * 1000 - now) / 1000));
    updateRoundTimerUI(secondsLeft);

    roundCountdownInterval = setInterval(() => {
        const now2 = Date.now();
        let left = Math.max(0, Math.floor((roundStart + roundTimeLimitSec * 1000 - now2) / 1000));
        updateRoundTimerUI(left);
        if (left <= 0) {
            clearInterval(roundCountdownInterval);
            localStudentFinished = true; // Mark as finished on timeout
            renderStudentList();
            // Optionally notify backend of timeout
        }
    }, 1000);
}

// --- Final Stats Popup ---
async function showFinalStatsPopup(stats, isHost) {
    console.log("showFinalStatsPopup() called");
    statsPopupShown = true;
    const overlay = document.getElementById('stats-overlay');
    const table = document.getElementById('stats-table');
    // Clear/hide the stats-popup-msg if present (fix redundant text)
    let msgDiv = document.getElementById('stats-popup-msg');
    if (msgDiv) msgDiv.textContent = '';
    // Sort by totalScore descending
    stats.sort((a, b) => b.totalScore - a.totalScore);
    table.innerHTML = `<tr><th>Rank</th><th>Student</th><th>Total Score</th><th>Guesses</th><th>Time (s)</th></tr>` +
        stats.map((row, idx) => {
            const timeSec = (row.timeTakenMillis / 1000).toFixed(2);
            return `<tr><td>${idx + 1}</td><td>${row.displayName || row.studentId}</td><td>${row.totalScore}</td><td>${row.guessCount}/6</td><td>${timeSec}</td></tr>`;
        }).join('');
    // Add Start New Class button for host
    let btnDiv = document.getElementById('final-stats-btn-div');
    if (!btnDiv) {
        btnDiv = document.createElement('div');
        btnDiv.id = 'final-stats-btn-div';
        btnDiv.style.margin = '16px 0 0 0';
        table.parentNode.appendChild(btnDiv);
    }
    btnDiv.innerHTML = '';
    // Place finalMsgDiv just above the Start New Class button
    let finalMsgDiv = document.getElementById('final-msg');
    if (!finalMsgDiv) {
        finalMsgDiv = document.createElement('div');
        finalMsgDiv.id = 'final-msg';
        finalMsgDiv.style.fontSize = '1.2em';
        finalMsgDiv.style.margin = '12px 0';
        finalMsgDiv.style.fontWeight = 'bold';
    }
    finalMsgDiv.textContent = 'Not tired yet... 😏';
    // Insert finalMsgDiv just before btnDiv
    if (btnDiv.parentNode && btnDiv.parentNode !== finalMsgDiv.parentNode) {
        btnDiv.parentNode.insertBefore(finalMsgDiv, btnDiv);
    }
    if (isHost) {
        const btn = document.createElement('button');
        btn.textContent = 'Start New Class';
        btn.className = 'dialog-btn';
        btn.onclick = async () => {
            btn.disabled = true;
            await fetch(`/wordlecup/clazz/${clazzId}/restart`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ clazzLeaderId: username })
            });
            // Wait a moment for backend to update, then reload
            setTimeout(() => { window.location.reload(); }, 500);
        };
        btnDiv.appendChild(btn);
    }
    overlay.classList.remove('hidden');
}

// --- WebSocket/clazz end logic ---
async function handleGameEnd() {
    const res = await fetch(`/wordlecup/clazz/${clazzId}/test-stats`);
    if (!res.ok) return;
    const stats = await res.json();
    // Fetch room info to check host
    const roomRes = await fetch(`/wordlecup/clazz/${clazzId}`);
    let isHost = false;
    if (roomRes.ok) {
        const room = await roomRes.json();
        isHost = room.clazzLeaderId === username;
    }
    await showFinalStatsPopup(stats, isHost);
}
