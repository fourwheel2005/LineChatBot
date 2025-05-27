package com.example.linechatbot.LineBot;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.bot.model.event.CallbackRequest;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;

import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class WebhookController {

    @Autowired
    private FollowService followService;

    @Autowired
    private MessageService messageService;

    @PostMapping("/callback")
    public String callback(@RequestBody String payload) throws Exception {
        ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
        CallbackRequest callbackRequest = objectMapper.readValue(payload, CallbackRequest.class);
        List<Event> events = callbackRequest.getEvents();

        for (Event event : events) {
            if (event instanceof MessageEvent<?> messageEvent) {
                CompletableFuture.runAsync(() -> messageService.handleMessageEvent(messageEvent));
            } else if (event instanceof FollowEvent followEvent) {
                CompletableFuture.runAsync(() -> followService.handleFollowEvent(followEvent));
            }
        }

        return "OK";
    }
}

