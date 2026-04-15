package com.geonexus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void subscribe(String userId, String subscriptionJson) {
        // 存储订阅信息到 Redis
        redisTemplate.opsForValue().set("push:sub:" + userId, subscriptionJson);
        log.info("User {} push subscription saved", userId);
    }

    public void unsubscribe(String userId) {
        redisTemplate.delete("push:sub:" + userId);
        log.info("User {} push subscription removed", userId);
    }

    public void sendNotification(String userId, String title, String body, String url) {
        String subscriptionJson = (String) redisTemplate.opsForValue().get("push:sub:" + userId);
        if (subscriptionJson == null) {
            log.warn("No push subscription for user {}", userId);
            return;
        }
        // 发送到 Web Push 队列（Kafka 或直接 HTTP）
        sendWebPush(subscriptionJson, title, body, url);
    }

    public void notifyTaskComplete(String userId, String taskId, String taskType) {
        sendNotification(userId,
            "任务完成",
            taskType + " 任务已完成，点击查看结果",
            "/tasks/" + taskId);
    }

    private void sendWebPush(String subscriptionJson, String title, String body, String url) {
        // 使用 web-push 库发送 VAPID 推送
        // 实际实现使用 web-push 或 Firebase Cloud Messaging
        log.info("Sending push notification to user: {}, title: {}", title, title);
    }
}
