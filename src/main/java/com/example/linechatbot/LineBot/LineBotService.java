package com.example.linechatbot.LineBot;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LineBotService {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    public void replyToUser(String replyToken, String message) {
        TextMessage reply = new TextMessage(message);
        ReplyMessage replyMessage = new ReplyMessage(replyToken, reply);
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pushFeedbackQuickReply(String userId) {
        List<QuickReplyItem> items = List.of(
                QuickReplyItem.builder().action(new MessageAction("✅ พอใจ", "พอใจ")).build(),
                QuickReplyItem.builder().action(new MessageAction("❌ ไม่พอใจ", "ไม่พอใจ")).build()
        );

        TextMessage feedbackMessage = TextMessage.builder()
                .text("ขอบคุณที่ติดต่อเรา 🙏🏻\nคุณพึงพอใจกับการให้บริการครั้งนี้หรือไม่?")
                .quickReply(QuickReply.items(items))
                .build();

        try {
            lineMessagingClient.pushMessage(new PushMessage(userId, feedbackMessage)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

