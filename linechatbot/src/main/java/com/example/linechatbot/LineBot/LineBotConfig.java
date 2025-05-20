package com.example.linechatbot.LineBot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.linecorp.bot.client.LineMessagingClient;

@Configuration
public class LineBotConfig {

    @Value("${line.bot.channel-token}")
    private String channelAccessToken;

    @Bean
    public LineMessagingClient lineMessagingClient() {
        return LineMessagingClient.builder(channelAccessToken).build();
    }
}
