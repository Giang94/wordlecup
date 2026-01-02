// Enable Create Room button only if username is at least 3 characters
const usernameInput = document.getElementById('usernameInput');
const createRoomBtn = document.getElementById('createRoomBtn');

usernameInput.addEventListener('input', function() {
    if (usernameInput.value.trim().length >= 3) {
        createRoomBtn.disabled = false;
    } else {
        createRoomBtn.disabled = true;
    }
});

const apiBase = '/wordlecup';

window.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const clazzId = params.get('clazzId');
    const welcomeContainer = document.getElementById('welcomeContainer');
    const roomJoinSection = document.getElementById('roomJoinSection');
    const roomJoinLabel = document.getElementById('roomJoinLabel');
    const joinUsernameInput = document.getElementById('joinUsernameInput');
    const joinRoomBtn = document.getElementById('joinRoomBtn');
    const joinRoomError = document.getElementById('joinRoomError');
    const roomInvalidSection = document.getElementById('roomInvalidSection');
    const createRoomBtn2 = document.getElementById('createRoomBtn2');
    const singleStudentSection = document.getElementById('singleStudentSection');

    if (clazzId) {
        // Hide single student section
        singleStudentSection.style.display = 'none';
        // Check if room exists
        try {
            const res = await fetch(apiBase + '/clazz/' + encodeURIComponent(clazzId));
            if (res.ok) {
                roomJoinSection.style.display = '';
                roomJoinLabel.innerHTML = `You are joining room <b>${clazzId.toUpperCase()}</b>`;
                joinUsernameInput.value = '';
                joinRoomBtn.disabled = true;
                joinRoomError.innerText = '';
                joinUsernameInput.addEventListener('input', function() {
                    joinRoomBtn.disabled = joinUsernameInput.value.trim().length < 3;
                });
                joinRoomBtn.onclick = async function() {
                    const displayName = joinUsernameInput.value.trim();
                    if (!displayName) {
                        joinRoomError.innerText = 'Please enter your student name.';
                        return;
                    }
                    // Try to join room
                    const joinRes = await fetch(apiBase + `/clazz/${clazzId}/join`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ studentId, displayName })
                    });
                    if (joinRes.ok) {
                        // Redirect to lobby
                        window.location.href = `/lobby.html?clazzId=${encodeURIComponent(clazzId)}&username=${encodeURIComponent(displayName)}`;
                    } else {
                        joinRoomError.innerText = 'Failed to join room.';
                    }
                };
            } else {
                // Room not found
                roomInvalidSection.style.display = '';
                roomJoinSection.style.display = 'none';
                singleStudentSection.style.display = 'none';
                createRoomBtn2.onclick = function() {
                    window.location.href = '/index.html';
                };
            }
        } catch (e) {
            roomInvalidSection.style.display = '';
            roomJoinSection.style.display = 'none';
            singleStudentSection.style.display = 'none';
            createRoomBtn2.onclick = function() {
                window.location.href = '/index.html';
            };
        }
    } else {
        // No clazzId, show single student section as normal
        singleStudentSection.style.display = '';
        roomJoinSection.style.display = 'none';
        roomInvalidSection.style.display = 'none';
    }
});

// On page load, check for clazzId in URL and show code if present
window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const clazzId = params.get('clazzId');
    if (clazzId) {
        document.getElementById('result').innerHTML =
            'Joining room: <b>' + clazzId.toUpperCase() + '</b>';
    }
});

document.getElementById('createRoomBtn').onclick = async function() {
    const username = document.getElementById('usernameInput').value.trim();
    if (!username) {
        document.getElementById('result').innerText = 'Please enter your username.';
        return;
    }
    const clazzLeaderId = username;
    const req = {
        clazzLeaderId: clazzLeaderId,
        maxStudents: 4,
        totalTests: 2,
        roundTimeLimitSec: 5,
        maxAttemptsPerRound: 6
    };
    const res = await fetch(apiBase + '/clazz', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req)
    });
    if (res.ok) {
        const data = await res.json();
        // Redirect to lobby page with clazzId and username
        window.location.href = `/lobby.html?clazzId=${encodeURIComponent(data.clazzId)}&username=${encodeURIComponent(username)}`;
    } else {
        document.getElementById('result').innerText = 'Failed to create room.';
    }
};
