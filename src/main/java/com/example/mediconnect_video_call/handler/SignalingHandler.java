package com.example.mediconnect_video_call.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // Map of appointmentId -> Set of WebSocketSession
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Connection established, but we don't know the room yet until JOIN message
        System.out.println("New WebSocket connection: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        SignalingMessage signalingMessage = objectMapper.readValue(payload, SignalingMessage.class);
        String appointmentId = signalingMessage.getAppointmentId();

        if (appointmentId == null) {
            System.err.println("No appointmentId in message");
            return;
        }

        switch (signalingMessage.getType()) {
            case "JOIN":
                handleJoin(session, appointmentId);
                break;
            case "OFFER":
            case "ANSWER":
            case "ICE":
                handleForwarding(session, appointmentId, message);
                break;
            default:
                System.err.println("Unknown message type: " + signalingMessage.getType());
        }
    }

    private void handleJoin(WebSocketSession session, String appointmentId) throws IOException {
        rooms.computeIfAbsent(appointmentId, k -> Collections.synchronizedSet(new HashSet<>()));
        Set<WebSocketSession> room = rooms.get(appointmentId);

        if (room.size() >= 2) {
            System.out.println("Room " + appointmentId + " is full");
            return;
        }

        // Notify existing members that someone joined
        for (WebSocketSession s : room) {
            if (s.isOpen()) {
                String userJoinedMsg = String.format("{\"type\":\"USER_JOINED\", \"appointmentId\":\"%s\"}",
                        appointmentId);
                s.sendMessage(new TextMessage(userJoinedMsg));
            }
        }

        room.add(session);
        session.getAttributes().put("appointmentId", appointmentId);
        System.out.println("Session " + session.getId() + " joined room " + appointmentId);

        // Notify the new member about the room state
        boolean hasPeer = room.size() > 1;
        String roomJoinedMsg = String.format("{\"type\":\"ROOM_JOINED\", \"appointmentId\":\"%s\", \"hasPeer\":%b}",
                appointmentId, hasPeer);
        session.sendMessage(new TextMessage(roomJoinedMsg));
    }

    private void handleForwarding(WebSocketSession session, String appointmentId, TextMessage message)
            throws IOException {
        Set<WebSocketSession> room = rooms.get(appointmentId);
        if (room != null) {
            System.out.println("Forwarding message " + message.getPayload() + " to room " + appointmentId);
            for (WebSocketSession s : room) {
                // Don't echo back to sender
                if (s.isOpen() && !s.getId().equals(session.getId())) {
                    s.sendMessage(message);
                    System.out.println("Sent to session: " + s.getId());
                }
            }
        } else {
            System.err.println("Room not found for forwarding: " + appointmentId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String appointmentId = (String) session.getAttributes().get("appointmentId");
        if (appointmentId != null) {
            Set<WebSocketSession> room = rooms.get(appointmentId);
            if (room != null) {
                room.remove(session);
                System.out.println("Session " + session.getId() + " removed from room " + appointmentId);

                if (room.isEmpty()) {
                    rooms.remove(appointmentId);
                    System.out.println("Room " + appointmentId + " is empty and removed");
                }
            }
        }
        super.afterConnectionClosed(session, status);
    }

    // Inner class for message mapping
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SignalingMessage {
        private String type;
        private String appointmentId;
        private Object sdp;
        private Object candidate;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAppointmentId() {
            return appointmentId;
        }

        public void setAppointmentId(String appointmentId) {
            this.appointmentId = appointmentId;
        }

        public Object getSdp() {
            return sdp;
        }

        public void setSdp(Object sdp) {
            this.sdp = sdp;
        }

        public Object getCandidate() {
            return candidate;
        }

        public void setCandidate(Object candidate) {
            this.candidate = candidate;
        }
    }
}
