package com.example.linechatbot.LineBot;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class WebhookController {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    private final ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();

    @PostMapping("/callback")
    public String callback(@RequestBody String payload) throws Exception {
        CallbackRequest callbackRequest = objectMapper.readValue(payload, CallbackRequest.class);
        List<Event> events = callbackRequest.getEvents();

        for (Event event : events) {
            if (event instanceof MessageEvent) {
                handleMessageEvent((MessageEvent<?>) event);
            }
        }

        return "OK";
    }

    private void handleMessageEvent(MessageEvent<?> event) {
        if (event.getMessage() instanceof TextMessageContent) {
            TextMessageContent messageContent = (TextMessageContent) event.getMessage();
            String replyToken = event.getReplyToken();
            String userMessage = messageContent.getText();

            String replyText = processBusinessLogic(userMessage);
            replyToUser(replyToken, replyText);
        }
    }

    private String processBusinessLogic(String input) {
        int maxRetries = 3;
        int retryDelayMillis = 2000; // หน่วงเวลา 2 วินาที

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                URL url = new URL("https://intent-api.onrender.com/analyze");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);

                String jsonInput = "{\"text\":\"" + input + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] inputBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int status = conn.getResponseCode();

                if (status == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }

                    String intent = new ObjectMapper().readTree(response.toString()).get("intent").asText();

                    return switch (intent) {
                        case "contact" -> "คุณสามารถติดต่อเราได้ที่เบอร์โทร: 089-968-6309 หรือ Line ID: @capseal";
                        case "price" -> "ราคาแคปซีลเริ่มต้นที่ 1.50 บาทต่อชิ้น (ขึ้นอยู่กับขนาดและปริมาณสั่ง) ครับ";
                        case "order" -> "หากสนใจสั่งซื้อสามารถติดต่อผ่าน Line ID: @capseal หรือโทร 089-968-6309 ได้เลยครับ";
                        default -> "สวัสดีครับ ยินดีต้อนรับสู่บริษัทสรรชัยพลาสติกมั่นคงจำกัด หากท่านต้องการสอบถามเกี่ยวกับแคปซีล เช่น ราคา เบอร์โทร หรือสั่งซื้อ พิมพ์มาได้เลยครับ!";
                    };
                } else {
                    System.out.println("Attempt " + attempt + " failed with HTTP " + status);
                }

            } catch (IOException e) {
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
            }

            try {
                Thread.sleep(retryDelayMillis); // หน่วงก่อน retry
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
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
