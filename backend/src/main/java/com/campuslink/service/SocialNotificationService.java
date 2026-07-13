package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.NotificationSummary;
import com.campuslink.dto.SocialNotificationDtos.NotificationView;
import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.repository.SocialNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialNotificationService {

  private final SocialNotificationRepository notifications;

  public SocialNotificationService(SocialNotificationRepository notifications) {
    this.notifications = notifications;
  }

  public NotificationSummary summary(String recipientId) {
    return new NotificationSummary(
        notifications.findForRecipient(recipientId).stream().map(this::view).toList(),
        notifications.countUnread(recipientId));
  }

  public void recordPostLiked(
      String recipientId, String actorId, String actorName, Long postId) {
    notifications.create(
        recipientId,
        actorId,
        String.valueOf(postId),
        "social.post.liked",
        "动态收到新点赞",
        actorName + "赞了你的动态。");
  }

  @Transactional
  public NotificationSummary markAllRead(String recipientId) {
    notifications.markAllRead(recipientId);
    return summary(recipientId);
  }

  private NotificationView view(SocialNotificationEntity notification) {
    return new NotificationView(
        notification.id(),
        notification.targetId(),
        notification.type(),
        notification.title(),
        notification.body(),
        notification.readAt() != null,
        notification.createdAt());
  }
}
