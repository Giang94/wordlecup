package com.app.wordlecup.cup.config;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClazzWebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clazzId = getRoomId(session);
        roomSessions.computeIfAbsent(clazzId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clazzId = getRoomId(session);
        Set<WebSocketSession> sessions = roomSessions.get(clazzId);
        if (sessions != null) sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Optionally handle client messages
    }

    public static void broadcastToRoom(String clazzId, String payload) {
        Set<WebSocketSession> sessions = roomSessions.get(clazzId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private String getRoomId(WebSocketSession session) {
        String uri = session.getUri().toString();
        String[] parts = uri.split("clazzId=");
        if (parts.length > 1) return parts[1].split("&")[0];
        return "default";
    }
}

