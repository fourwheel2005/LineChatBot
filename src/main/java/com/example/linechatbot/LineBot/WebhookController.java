package com.example.linechatbot.LineBot;

import com.example.linechatbot.User.model.User;
import com.example.linechatbot.User.model.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.CallbackRequest;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class WebhookController {

    @Autowired
    private LineMessagingClient lineMessagingClient;
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();



    @PostMapping("/callback")
    public String callback(@RequestBody String payload) throws Exception {
        CallbackRequest callbackRequest = objectMapper.readValue(payload, CallbackRequest.class);
        List<Event> events = callbackRequest.getEvents();

        for (Event event : events) {
            if (event instanceof MessageEvent) {
                // ใช้ async ทำงาน background
                CompletableFuture.runAsync(() -> handleMessageEvent((MessageEvent<?>) event));
            }
        }

        return "OK"; // ตอบกลับไวทันที ไม่รอ processBusinessLogic
    }



    private void handleMessageEvent(MessageEvent<?> event) {
        if (event.getMessage() instanceof TextMessageContent) {
            TextMessageContent messageContent = (TextMessageContent) event.getMessage();
            String replyToken = event.getReplyToken();
            String userMessage = messageContent.getText();
            String userId = event.getSource().getUserId();

            try {
                UserProfileResponse profile = lineMessagingClient.getProfile(userId).get();

                if (!userRepository.existsById(userId)) {
                    User newUser = new User();
                    newUser.setUserId(userId);
                    newUser.setName(profile.getDisplayName());
                    newUser.setDisplayName(profile.getDisplayName());
                    newUser.setCreatedAt(LocalDateTime.now());
                    userRepository.save(newUser);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            String replyText = processBusinessLogic(userMessage);
            replyToUser(replyToken, replyText);
        }
    }




    private String processBusinessLogic(String input) {
        int maxRetries = 2;
        int retryDelayMillis = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                URL url = new URL("https://intent-api.onrender.com/analyze");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Java-LineBot/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);

                String jsonInput = "{\"text\":\"" + input + "\"}";
                System.out.println("Calling: " + url);
                System.out.println("Payload: " + jsonInput);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] inputBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int status = conn.getResponseCode();
                System.out.println("HTTP Status: " + status);

                if (status == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }

                    JsonNode node = new ObjectMapper().readTree(response.toString());
                    String intent = node.has("intent") ? node.get("intent").asText() : "unknown";

                    return switch (intent) {
                        case "contact" -> "คุณสามารถติดต่อเราได้ที่เบอร์โทร: 089-968-6309 หรือ Line ID: @capseal";
                        case "price" -> "ราคาแคปซีลเริ่มต้นที่ 1.50 บาทต่อชิ้น (ขึ้นอยู่กับขนาดและปริมาณสั่ง) ครับ";
                        case "order" -> "หากสนใจสั่งซื้อสามารถติดต่อผ่าน Line ID: @capseal หรือโทร 089-968-6309 ได้เลยครับ";
                        default -> "สวัสดีครับ ยินดีต้อนรับสู่บริษัทสรรชัยพลาสติกมั่นคงจำกัด หากท่านต้องการสอบถามเกี่ยวกับแคปซีล เช่น ราคา เบอร์โทร หรือสั่งซื้อ พิมพ์มาได้เลยครับ!";
                    };
                } else {
                    System.out.println("Attempt " + attempt + " failed with HTTP " + status);
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorResponse.append(line.trim());
                        }
                        System.out.println("Error Body: " + errorResponse.toString());
                    }
                }

            } catch (IOException e) {
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
            }

            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return "ขออภัย ระบบไม่สามารถประมวลผลคำถามของคุณได้ในขณะนี้ครับ";
    }


    private void replyToUser(String replyToken, String message) {
        TextMessage reply = new TextMessage(message);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, reply);
        try {
            lineMessagingClient.replyMessage(replyMessage).get(); // ยัง sync อยู่ แต่ใช้ใน background แล้ว
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
