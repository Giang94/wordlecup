const apiBase = '/wordlecup';
const params = new URLSearchParams(window.location.search);
const clazzId = params.get('clazzId');
const username = params.get('username');

if (!clazzId) {
    document.body.innerHTML = '<h2>Room ID missing.</h2>';
    throw new Error('Room ID missing');
}

document.getElementById('roomCode').innerText = clazzId;

let isHost = false;
let studentId = username || ("guest_" + Math.floor(Math.random() * 1000000));
let clazzLeaderId = null;

// Join the clazz (if not already joined)
async function joinRoom() {
    try {
        const res = await fetch(`${apiBase}/clazz/${clazzId}/join`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId, displayName })
        });
        if (res.status === 404) {
            window.location.href = '/index.html';
            return;
        }
        if (!res.ok) {
            document.getElementById('lobbyStatus').innerText = 'Failed to join clazz.';
            throw new Error('Join failed');
        }
        const data = await res.json();
        isHost = data.isHost || false;
    } catch (e) {
        // Already joined or error
    }
}

// Fetch student list and clazz status
async function fetchLobby() {
    const res = await fetch(`${apiBase}/clazz/${clazzId}`);
    if (res.status === 404) {
        window.location.href = '/index.html';
        return;
    }
    if (!res.ok) {
        document.getElementById('lobbyStatus').innerText = 'Room not found or already started.';
        return;
    }
    const clazz = await res.json();
    if (clazz.status !== 'WAITING') {
        window.location.href = `/wordle.html?clazzId=${clazzId}&username=${encodeURIComponent(studentId)}`;
        return;
    }
    const studentList = document.getElementById('studentList');
    studentList.innerHTML = '';
    clazzLeaderId = clazz.clazzLeaderId || null;
    const students = Object.values(clazz.students);
    document.getElementById('studentCount').innerText = students.length;
    document.getElementById('lobbyTitle').innerText = `Lobby - ${clazzId} (${students.length})`;
    document.getElementById('lobbyHeader').innerHTML = `Lobby - <span id="roomCode">${clazzId}</span> (<span id="studentCount">${students.length}</span>)`;
    students.forEach(student => {
        const li = document.createElement('li');
        let text = student.studentId;
        if (clazzLeaderId && student.studentId === clazzLeaderId) {
            text += ' (host)';
        }
        // Show tick/cross if student has finished
        if (student.status === 'WIN') {
            text += ' ✓';
        } else if (student.status === 'LOSE') {
            text += ' ✗';
        }
        li.innerText = text;
        studentList.appendChild(li);
    });
    // Show Start Class button only for host, show waiting text for others
    const startBtn = document.getElementById('startClassBtn');
    const lobbyStatus = document.getElementById('lobbyStatus');
    if (clazzLeaderId && studentId === clazzLeaderId) {
        startBtn.style.display = '';
        lobbyStatus.innerText = '';
    } else {
        startBtn.style.display = 'none';
        lobbyStatus.innerText = 'Waiting for host to start...';
    }

    updateJoinLink();
}

// Update join link
const joinLinkInput = document.getElementById('joinLink');
const copyJoinLinkBtn = document.getElementById('copyJoinLinkBtn');
const joinLinkContainer = document.getElementById('joinLinkContainer');
function updateJoinLink() {
    const baseUrl = window.location.origin + window.location.pathname.replace(/\/[^/]*$/, '/index.html');
    const link = `${baseUrl}?clazzId=${encodeURIComponent(clazzId)}`;
    joinLinkInput.value = link;
    joinLinkContainer.style.display = '';
}
updateJoinLink();
copyJoinLinkBtn.onclick = function () {
    joinLinkInput.select();
    joinLinkInput.setSelectionRange(0, 99999);
    document.execCommand('copy');
    copyJoinLinkBtn.textContent = 'Copied!';
    setTimeout(() => copyJoinLinkBtn.textContent = 'Copy', 1200);
}

// Remove student from clazz on unload
window.addEventListener('beforeunload', async () => {
    await fetch(`${apiBase}/clazz/${clazzId}/leave`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentId })
    });
});

document.getElementById('startClassBtn').onclick = async function () {
    // Only host can start
    const res = await fetch(`${apiBase}/clazz/${clazzId}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clazzLeaderId: studentId })
    });
    if (res.ok) {
        // Redirect to game page
        window.location.href = `/wordle.html?clazzId=${clazzId}&username=${encodeURIComponent(studentId)}`;
    } else {
        document.getElementById('lobbyStatus').innerText = 'Failed to start class.';
    }
};

// Poll for student list updates
setInterval(fetchLobby, 2000);

// Initial join and fetch
joinRoom().then(fetchLobby);
