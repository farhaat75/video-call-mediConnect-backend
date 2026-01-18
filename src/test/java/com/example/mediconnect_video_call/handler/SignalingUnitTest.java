package com.example.mediconnect_video_call.handler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

public class SignalingUnitTest {

    @Test
    public void testJoinAndForward() throws Exception {
        SignalingHandler handler = new SignalingHandler();

        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);

        when(sessionA.getId()).thenReturn("sessionA");
        when(sessionB.getId()).thenReturn("sessionB");
        when(sessionA.isOpen()).thenReturn(true);
        when(sessionB.isOpen()).thenReturn(true);

        Map<String, Object> attrsA = new ConcurrentHashMap<>();
        Map<String, Object> attrsB = new ConcurrentHashMap<>();
        when(sessionA.getAttributes()).thenReturn(attrsA);
        when(sessionB.getAttributes()).thenReturn(attrsB);

        String appointmentId = "unit-test-appt";

        // 1. Join A
        String joinMsg = String.format("{\"type\":\"JOIN\", \"appointmentId\":\"%s\"}", appointmentId);
        handler.handleTextMessage(sessionA, new TextMessage(joinMsg));

        Assertions.assertEquals(appointmentId, attrsA.get("appointmentId"));

        // 2. Join B
        handler.handleTextMessage(sessionB, new TextMessage(joinMsg));
        Assertions.assertEquals(appointmentId, attrsB.get("appointmentId"));

        // 3. Offer A -> B
        String offerPayload = "{\"sdp\":\"offer\"}";
        String offerMsg = String.format("{\"type\":\"OFFER\", \"appointmentId\":\"%s\", \"payload\":%s}", appointmentId,
                offerPayload);

        handler.handleTextMessage(sessionA, new TextMessage(offerMsg));

        // Verify B received the message
        verify(sessionB, times(1)).sendMessage(any(TextMessage.class));
        // Verify A did not receive it
        verify(sessionA, never()).sendMessage(any(TextMessage.class));

        // 4. Disconnect A
        handler.afterConnectionClosed(sessionA, CloseStatus.NORMAL);

        // 5. Answer B -> A (should fail or log since A is gone)
        // Actually A is removed from room, so B sends message to room members != B.
        // Room is now just B?
        // Wait, if A leaves, room has B. B sends message.
        // If A is gone, sending to A is impossible.
        // Let's verify room size logic via reflection or just behavior.

        // If Client B sends message now, it goes nowhere.
        String answerMsg = String.format("{\"type\":\"ANSWER\", \"appointmentId\":\"%s\", \"payload\":{}}",
                appointmentId);
        handler.handleTextMessage(sessionB, new TextMessage(answerMsg));

        verify(sessionA, never()).sendMessage(any(TextMessage.class));
    }
}
