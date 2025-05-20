package com.example.linechatbot.LineBot;

import com.example.linechatbot.User.model.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.CallbackRequest;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.message.flex.component.*;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



import java.util.*;

@RestController
public class WebhookController {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    private final ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
    private final UserService userService;

    public WebhookController(UserService userService) {
        this.userService = userService;
    }

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
            String replyToken = event.getReplyToken();
            String userMessage = ((TextMessageContent) event.getMessage()).getText().toLowerCase();

            // ดึง userId และขอ LINE profile
            String userId = event.getSource().getUserId();
            lineMessagingClient.getProfile(userId).whenComplete((profile, ex) -> {
                if (ex == null) {
                    String displayName = profile.getDisplayName();
                    // 💾 Save เข้า DB
                    userService.saveUserIfNotExists(userId, displayName);
                }
            });

            if (userMessage.contains("ราคา")) {
                replyWithFlexMessage(replyToken);
            } else {
                String replyText = processBusinessLogic(userMessage);
                replyToUser(replyToken, replyText);
            }
        }
    }

    private final Map<String, List<String>> keywordResponses = Map.of(
            "ติดต่อ", List.of(
                    "📞 ติดต่อเราได้ที่ 089-968-6309 หรือ Line ID: @capseal",
                    "💬 แอดไลน์ได้เลยที่ @capseal หรือโทร 089-968-6309"
            ),
            "โทร", List.of(
                    "📱 เบอร์โทรของเราคือ 089-968-6309 ครับ",
                    "☎️ สนใจติดต่อ โทรมาได้เลยที่ 089-968-6309"
            ),
            "สั่งซื้อ", List.of(
                    "🛒 สนใจสั่งซื้อ ติดต่อผ่าน Line ID: @capseal หรือโทร 089-968-6309 ได้เลยครับ",
                    "📦 สั่งซื้อได้เลยทางไลน์หรือโทรหาเรา!"
            )
    );

    private String processBusinessLogic(String input) {
        for (Map.Entry<String, List<String>> entry : keywordResponses.entrySet()) {
            if (input.contains(entry.getKey())) {
                return getRandomResponse(entry.getValue());
            }
        }

        return "คุณพิมพ์ว่า: \"" + input + "\" หากต้องการสอบถามเกี่ยวกับแคปซีล เช่น ราคา เบอร์โทร หรือสั่งซื้อ พิมพ์มาได้เลยครับ 😊";
    }

    private String getRandomResponse(List<String> responses) {
        Random rand = new Random();
        return responses.get(rand.nextInt(responses.size()));
    }

    private void replyToUser(String replyToken, String message) {
        TextMessage reply = new TextMessage(message);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, reply);
        lineMessagingClient.replyMessage(replyMessage).whenComplete((res, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            }
        });
    }

    private void replyWithFlexMessage(String replyToken) {
        Bubble bubble = Bubble.builder()
                .header(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(List.of(
                                Text.builder().text("📦 ข้อมูลแคปซีล").weight(Text.TextWeight.BOLD).size(FlexFontSize.XL).build()
                        ))
                        .build())
                .body(Box.builder()
                        .layout(FlexLayout.VERTICAL)
                        .contents(List.of(
                                Text.builder().text("💰 ราคาเริ่มต้น: 1.50 บาท/ชิ้น").size(FlexFontSize.Md).wrap(true).build(),
                                Text.builder().text("📞 โทร: 089-968-6309").size(FlexFontSize.Md).wrap(true).build(),
                                Text.builder().text("💬 Line ID: @capseal").size(FlexFontSize.Md).wrap(true).build()
                        ))
                        .build())
                .build();


        FlexContainer flexContainer = bubble;
        FlexMessage flexMessage = new FlexMessage("รายละเอียดแคปซีล", flexContainer);

        lineMessagingClient.replyMessage(new ReplyMessage(replyToken, flexMessage)).whenComplete((res, ex) -> {
            if (ex != null) ex.printStackTrace();
        });
    }
}
