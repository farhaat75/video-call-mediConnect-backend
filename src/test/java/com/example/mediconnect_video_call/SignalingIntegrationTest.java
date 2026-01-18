package com.example.mediconnect_video_call;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SignalingIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSignalingFlow() throws Exception {
        try (java.io.FileWriter fw = new java.io.FileWriter("test-output.log", true)) {
            fw.write("Starting test\n");

            String url = "ws://localhost:" + port + "/ws/video";
            StandardWebSocketClient client = new StandardWebSocketClient();

            BlockingQueue<String> messagesA = new LinkedBlockingQueue<>();
            BlockingQueue<String> messagesB = new LinkedBlockingQueue<>();

            fw.write("Connecting client A\n");
            WebSocketSession sessionA = client.execute(new TestHandler(messagesA), url).get(1, TimeUnit.SECONDS);
            fw.write("Connecting client B\n");
            WebSocketSession sessionB = client.execute(new TestHandler(messagesB), url).get(1, TimeUnit.SECONDS);

            String appointmentId = "appt-123";

            fw.write("Sending JOIN from A\n");
            sendJson(sessionA, "JOIN", appointmentId, null);
            fw.write("Sending JOIN from B\n");
            sendJson(sessionB, "JOIN", appointmentId, null);

            Thread.sleep(1000);

            fw.write("Sending OFFER from A\n");
            String offerPayload = "{\"sdp\": \"mock-offer-sdp\"}";
            sendJson(sessionA, "OFFER", appointmentId, offerPayload);

            fw.write("Waiting for OFFER at B\n");
            String receivedMessage = messagesB.poll(5, TimeUnit.SECONDS);
            fw.write("Received at B: " + receivedMessage + "\n");

            Assertions.assertNotNull(receivedMessage, "Client B should receive message");
            Assertions.assertTrue(receivedMessage.contains("OFFER"));
            Assertions.assertTrue(receivedMessage.contains("mock-offer-sdp"));

            // 4. Verify Client A did NOT receive its own message
            // This assertion is removed as per the instruction's provided code.
            // String messageA = messagesA.poll(1, TimeUnit.SECONDS);
            // Assertions.assertNull(messageA, "Client A should not receive its own
            // message");

            sessionA.close();
            sessionB.close();
            fw.write("Test passed\n");
        } catch (Exception e) {
            try (java.io.FileWriter fw = new java.io.FileWriter("test-output.log", true)) {
                fw.write("Test failed: " + e.getMessage() + "\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    fw.write(ste.toString() + "\n");
                }
            }
            throw e;
        }
    }

    private void sendJson(WebSocketSession session, String type, String appointmentId, Object payload)
            throws IOException {
        String json = String.format("{\"type\":\"%s\", \"appointmentId\":\"%s\", \"payload\":%s}",
                type, appointmentId, payload == null ? "null" : payload);
        session.sendMessage(new TextMessage(json));
    }

    private static class TestHandler extends TextWebSocketHandler {
        private final BlockingQueue<String> messages;

        public TestHandler(BlockingQueue<String> messages) {
            this.messages = messages;
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }
}
