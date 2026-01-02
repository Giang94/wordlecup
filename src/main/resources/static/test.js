const apiBase = '/wordlecup'; // Change if your API base path is different

function createRoom() {
    const clazzLeaderId = document.getElementById('clazzLeaderId').value;
    const ownerDisplayName = document.getElementById('ownerDisplayName').value;
    fetch(`${apiBase}/clazz`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clazzLeaderId, ownerDisplayName })
    })
    .then(r => r.json())
    .then(data => {
        document.getElementById('result').innerText = 'Room created: ' + data.clazzId;
        document.getElementById('clazzId').value = data.clazzId;
        document.getElementById('startRoomId').value = data.clazzId;
        document.getElementById('playRoomId').value = data.clazzId;
    })
    .catch(e => alert('Error: ' + e));
}

function joinRoom() {
    const clazzId = document.getElementById('clazzId').value;
    const studentId = document.getElementById('studentId').value;
    const displayName = document.getElementById('displayName').value;
    fetch(`${apiBase}/clazz/${clazzId}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentId, displayName })
    })
    .then(r => r.json())
    .then(data => {
        document.getElementById('result').innerText = 'Joined room as: ' + data.displayName;
        document.getElementById('playStudentId').value = data.studentId;
    })
    .catch(e => alert('Error: ' + e));
}

function startClass() {
    const clazzId = document.getElementById('startRoomId').value;
    const clazzLeaderId = document.getElementById('startOwnerId').value;
    fetch(`${apiBase}/clazz/${clazzId}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clazzLeaderId })
    })
    .then(r => r.json())
    .then(data => {
        document.getElementById('result').innerText = 'Class started!';
    })
    .catch(e => alert('Error: ' + e));
}

function submitGuess() {
    const clazzId = document.getElementById('playRoomId').value;
    const studentId = document.getElementById('playStudentId').value;
    const word = document.getElementById('guessWord').value;
    fetch(`${apiBase}/clazz/${clazzId}/guess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clazzId, studentId, word })
    })
    .then(r => r.json())
    .then(data => {
        showGuessResult(word, data.letterResults, data.win, data.finished);
    })
    .catch(e => alert('Error: ' + e));
}

function showGuessResult(word, letterResults, win, finished) {
    const guessesDiv = document.getElementById('guesses');
    const row = document.createElement('div');
    row.className = 'guess-row';
    letterResults.forEach((lr, i) => {
        const span = document.createElement('span');
        span.className = 'letter ' + lr.status;
        span.innerText = lr.letter;
        row.appendChild(span);
    });
    guessesDiv.appendChild(row);
    let msg = '';
    if (win) msg = 'You WON!';
    else if (finished) msg = 'Test finished!';
    document.getElementById('result').innerText = msg;
}