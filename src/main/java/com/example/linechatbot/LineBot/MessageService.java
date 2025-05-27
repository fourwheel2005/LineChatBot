package com.example.linechatbot.LineBot;

import com.example.linechatbot.User.model.User;
import com.example.linechatbot.User.model.UserRepository;
import com.example.linechatbot.feedback.FeedBack;
import com.example.linechatbot.feedback.FeedBackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.profile.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class MessageService {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedBackRepository feedBackRepository;

    @Autowired
    private LineBotService lineBotService;

    public void handleMessageEvent(MessageEvent<?> event) {
        if (!(event.getMessage() instanceof TextMessageContent messageContent)) return;

        String replyToken = event.getReplyToken();
        String userMessage = messageContent.getText();
        String userId = event.getSource().getUserId();

        try {
            UserProfileResponse profile = lineMessagingClient.getProfile(userId).get();
            LocalDateTime now = LocalDateTime.now();
            boolean showWelcomeBack = false;

            if ("พอใจ".equalsIgnoreCase(userMessage) || "ไม่พอใจ".equalsIgnoreCase(userMessage)) {
                FeedBack feedback = FeedBack.builder()
                        .userId(userId)
                        .displayName(profile.getDisplayName())
                        .sentiment(userMessage)
                        .createdAt(now)
                        .build();
                feedBackRepository.save(feedback);

                String response = "พอใจ".equalsIgnoreCase(userMessage)
                        ? "ขอบคุณสำหรับคำติชมครับ 😊"
                        : "ขออภัยในความไม่สะดวกครับ 🙏 ทางเราจะนำข้อเสนอแนะไปปรับปรุงให้ดียิ่งขึ้น";

                lineBotService.replyToUser(replyToken, response);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                user = new User();
                user.setUserId(userId);
                user.setName(profile.getDisplayName());
                user.setDisplayName(profile.getDisplayName());
                user.setCreatedAt(now);
                showWelcomeBack = true;
            } else {
                if (user.getLastMessageAt() == null || user.getLastMessageAt().isBefore(now.minusDays(1))) {
                    showWelcomeBack = true;
                }
            }

            user.setLastMessageAt(now);
            userRepository.save(user);

            String replyText = processBusinessLogic(userMessage);
            if (showWelcomeBack) {
                String welcomeBackMessage = String.format("""
                    👋 ยินดีต้อนรับกลับมาครับคุณ %s  
                    หากมีคำถามเกี่ยวกับแคปซีล เช่น ราคา สั่งซื้อ หรือขนส่ง สามารถพิมพ์เข้ามาได้เลยครับ 😊
                    """, user.getDisplayName());
                replyText = welcomeBackMessage + "\n\n" + replyText;
            }

            lineBotService.replyToUser(replyToken, replyText);
            lineBotService.pushFeedbackQuickReply(userId);

        } catch (Exception e) {
            e.printStackTrace();
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
                        case "contact" -> """
        📞 คุณสามารถติดต่อเราได้ที่  
        ☎️ เบอร์โทร: 089-968-6309  
        💬 Line ID: @capseal
        """;

                        case "price" -> """
        💰 ราคาของแต่ละชิ้นอาจแตกต่างกันไป  
        รบกวนแจ้งสินค้าที่สนใจ เช่น  
        ▸ แคปซีลหุ้มขวด  
        ▸ แคปซีลคอถัง  

        เพื่อให้ทางเราสามารถแจ้งราคาได้อย่างแม่นยำครับ  
        สนใจสอบถามเพิ่มเติมได้เลยครับ 😊
        """;


                        case "order" -> """
        🛒 หากสนใจสั่งซื้อ  
        📲 ทัก Line ID: @capseal  
        📞 หรือโทร: 089-968-6309 ได้เลยครับ
        """;

                        case "companyAdvantages" -> """
        🏭 บริษัท **สรรชัยพลาสติกมั่นคง**  
        ✅ ดำเนินธุรกิจมากว่า 10 ปี  
        🧠 เชี่ยวชาญด้านการผลิตและจำหน่ายแคปซีลคุณภาพสูง  
        👥 ลูกค้ามากกว่า 1,000 รายทั่วประเทศให้ความไว้วางใจ  
        📦 มาตรฐานการผลิตระดับอุตสาหกรรม
        """;

                        case "product" -> """
        📦 เราจำหน่ายแคปซีล (Capseal) หลากหลายขนาด  
        📏 ตั้งแต่ 18mm จนถึง 35mm  
        🧴 สำหรับขวด PET, ขวดแก้ว และบรรจุภัณฑ์ต่าง ๆ  
        📸 สนใจขอแคตตาล็อกหรือดูตัวอย่างสินค้าได้เลยครับ
        """;

                        case "payment" -> """
        💳 ช่องทางชำระเงินของบริษัทมีดังนี้ครับ  

        🏦 ธนาคารกสิกรไทย  
        🔢 เลขบัญชี: 0702751624 
        👤 ชื่อบัญชี: สรรชัย ทรงศิลป์  

        ✅ รองรับการโอนเงิน  
        📦 มีบริการเก็บเงินปลายทาง (COD) ด้วยครับ
        """;

                        case "delivery" -> """
        🚚 ทางเราจัดส่งสินค้าทั่วประเทศ  
        🏤 ขนส่ง: Flash Express, Kerry, ไปรษณีย์ไทย ฯลฯ  
        ⏱ ระยะเวลาจัดส่ง: 1–3 วันทำการ  
        📦 หากต้องการ Tracking Number แจ้งทางแอดมินได้เลยครับ
        """;

                        case "working_hours" -> """
        🕘 เวลาทำการของบริษัทคือ  
        📅 จันทร์–เสาร์: 08:00–00:00 น.  
        ❌ หยุดวันอาทิตย์  
        💬 ทักแชทสอบถามนอกเวลาได้ แอดมินจะตอบกลับเร็วที่สุดครับ 😊
        """;

                        case "location" -> """
        📍 ที่ตั้งของบริษัทคือ  
        🏢 บริษัท สรรชัยพลาสติกมั่นคง จำกัด  
        🗺 117 ตำบลคลองเขิน อำเภอเมืองสมุทรสงคราม  
        จังหวัดสมุทรสงคราม 75000  

        🔗 ดูแผนที่ใน Google Maps:  
        https://maps.app.goo.gl/8uh1kepzkr1JQbZU8

        🙏 สามารถเข้ามารับสินค้าเองที่หน้าโรงงานได้ครับ  
        🕒 รบกวนนัดเวลาล่วงหน้าเพื่อเตรียมของไว้ให้
        """;

                        default -> """
        🙏🏻 สวัสดีครับ ยินดีต้อนรับสู่ **บริษัทสรรชัยพลาสติกมั่นคง**  
        หากต้องการสอบถามเรื่องแคปซีล เช่น  

        💰 ราคา  
        📞 เบอร์โทร  
        🛒 การสั่งซื้อ  
        📦 สินค้า  
        💳 ช่องทางการชำระเงิน  
        🚚 การจัดส่ง  
        🕘 เวลาเปิด-ปิด  
        📍 ที่อยู่บริษัท  

        ✨ พิมพ์ข้อความเข้ามาได้เลยครับ ยินดีให้บริการ 😊
        """;
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

        return "ขออภัย ระบบกำลังอยู่ระหว่างการปรับปรุงชั่วคราว ขออภัยในความไม่สะดวกครับ🙏🏻";
    }
}
