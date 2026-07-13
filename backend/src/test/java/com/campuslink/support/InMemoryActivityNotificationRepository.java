package com.campuslink.support;

import com.campuslink.entity.ActivityNotificationEntity;
import com.campuslink.repository.ActivityNotificationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InMemoryActivityNotificationRepository
    implements ActivityNotificationRepository {

  private final List<ActivityNotificationEntity> notifications = new ArrayList<>();

  @Override
  public ActivityNotificationEntity create(
      String recipientId, String activityId, String type, String title, String body) {
    ActivityNotificationEntity notification = new ActivityNotificationEntity(
        UUID.randomUUID().toString(), recipientId, activityId, type, title, body,
        null, LocalDateTime.now());
    notifications.add(notification);
    return notification;
  }

  @Override
  public List<ActivityNotificationEntity> findForRecipient(String recipientId) {
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
      ActivityNotificationEntity notification = notifications.get(index);
      if (notification.recipientId().equals(recipientId) && notification.readAt() == null) {
        notifications.set(index, new ActivityNotificationEntity(
            notification.id(), notification.recipientId(), notification.activityId(),
            notification.type(), notification.title(), notification.body(),
            LocalDateTime.now(), notification.createdAt()));
        updated++;
      }
    }
    return updated;
  }
}
