package com.example.linechatbot.LineBot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Component
public class FeedbackScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> feedbackTasks = new ConcurrentHashMap<>();

    @Autowired
    private LineBotService lineBotService;

    public void scheduleFeedback(String userId) {
        // ถ้ามี task เดิมอยู่ ให้ยกเลิกก่อน
        cancelFeedback(userId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            lineBotService.pushFeedbackQuickReply(userId);
            feedbackTasks.remove(userId); // ล้างออกเมื่อเสร็จ
        }, 30, TimeUnit.SECONDS);

        feedbackTasks.put(userId, future);
    }

    public void cancelFeedback(String userId) {
        ScheduledFuture<?> task = feedbackTasks.remove(userId);
        if (task != null) {
            task.cancel(false);
        }
    }
}

