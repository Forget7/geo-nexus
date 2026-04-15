package com.geonexus.service;

import com.geonexus.domain.NotificationEntity;
import com.geonexus.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final PushService pushService;

    public NotificationService(NotificationRepository repo, PushService pushService) {
        this.repo = repo;
        this.pushService = pushService;
    }

    public NotificationEntity create(NotificationEntity notification) {
        notification.setId(UUID.randomUUID().toString());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        NotificationEntity saved = repo.save(notification);
        try {
            pushService.sendNotification(
                notification.getUserId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getUrl()
            );
        } catch (Exception e) {
            log.warn("Failed to send push for notification {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    public Page<NotificationEntity> getUserNotifications(String userId, int page, int size) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Page<NotificationEntity> getUnread(String userId, int page, int size) {
        return repo.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, PageRequest.of(page, size));
    }

    public long getUnreadCount(String userId) {
        return repo.countByUserIdAndRead(userId, false);
    }

    public void markAsRead(String notificationId) {
        NotificationEntity n = repo.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        repo.save(n);
    }

    public void markAllAsRead(String userId) {
        repo.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, Pageable.unpaged())
            .forEach(n -> {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                repo.save(n);
            });
    }

    public void deleteNotification(String notificationId) {
        repo.deleteById(notificationId);
    }

    public void deleteAllForUser(String userId) {
        repo.deleteByUserId(userId);
    }
}
