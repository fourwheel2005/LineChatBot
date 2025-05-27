package com.example.linechatbot.LineBot;

import com.example.linechatbot.User.model.User;
import com.example.linechatbot.User.model.UserRepository;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FollowService {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LineBotService lineBotService;

    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
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

            String welcomeMessage = String.format("""
                สวัสดีครับคุณ %s 🙏🏻
                ยินดีต้อนรับสู่บริษัทสรรชัยพลาสติกมั่นคงครับ 🎉
                เราเป็นผู้เชี่ยวชาญด้านแคปซีล (Cap Seal) หากต้องการสอบถามข้อมูลเพิ่มเติม เช่น ราคา สั่งซื้อ หรือช่องทางติดต่อ สามารถพิมพ์ข้อความเข้ามาได้เลยครับ 😊
                """, profile.getDisplayName());

            lineBotService.replyToUser(replyToken, welcomeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

