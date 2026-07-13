package com.campuslink.support;

import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.repository.SocialNotificationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class InMemorySocialNotificationRepository implements SocialNotificationRepository {

  private final List<SocialNotificationEntity> notifications = new ArrayList<>();

  @Override
  public SocialNotificationEntity create(
      String recipientId,
      String actorId,
      String targetId,
      String type,
      String title,
      String body) {
    SocialNotificationEntity notification = new SocialNotificationEntity(
        "social-notification-" + (notifications.size() + 1),
        recipientId,
        actorId,
        targetId,
        type,
        title,
        body,
        null,
        LocalDateTime.now());
    notifications.add(notification);
    return notification;
  }

  @Override
  public List<SocialNotificationEntity> findForRecipient(String recipientId) {
    return notifications.reversed().stream()
        .filter(notification -> notification.recipientId().equals(recipientId))
        .toList();
  }

  @Override
  public int countUnread(String recipientId) {
    return (int) notifications.stream()
        .filter(notification -> notification.recipientId().equals(recipientId))
        .filter(notification -> notification.readAt() == null)
        .count();
  }

  @Override
  public int markAllRead(String recipientId) {
    int updated = 0;
    for (int index = 0; index < notifications.size(); index++) {
      SocialNotificationEntity notification = notifications.get(index);
      if (notification.recipientId().equals(recipientId) && notification.readAt() == null) {
        notifications.set(index, new SocialNotificationEntity(
            notification.id(), notification.recipientId(), notification.actorId(),
            notification.targetId(), notification.type(), notification.title(), notification.body(),
            LocalDateTime.now(), notification.createdAt()));
        updated++;
      }
    }
    return updated;
  }
}
