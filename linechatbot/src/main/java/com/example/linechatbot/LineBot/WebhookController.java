package com.example.linechatbot.LineBot;

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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    private final Map<String, String> keywordResponses = Map.of(
            "ติดต่อ", "คุณสามารถติดต่อเราได้ที่เบอร์โทร: 089-968-6309 หรือ Line ID: @capseal",
            "โทร", "เบอร์โทรสำหรับติดต่อคือ: 089-968-6309 ครับ",
            "ราคา", "ราคาแคปซีลเริ่มต้นที่ 1.50 บาทต่อชิ้น (ขึ้นอยู่กับขนาดและปริมาณสั่ง) ครับ",
            "สั่งซื้อ", "หากสนใจสั่งซื้อสามารถติดต่อผ่าน Line ID: @capseal หรือโทร 089-968-6309 ได้เลยครับ"
    );

    private String processBusinessLogic(String input) {
        String message = input.toLowerCase();

        for (Map.Entry<String, String> entry : keywordResponses.entrySet()) {
            if (message.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "คุณพิมพ์ว่า: " + input + " หากต้องการสอบถามเกี่ยวกับแคปซีล เช่น ราคา เบอร์โทร หรือสั่งซื้อ พิมพ์มาได้เลยครับ!";
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
