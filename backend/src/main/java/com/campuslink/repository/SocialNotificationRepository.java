package com.campuslink.repository;

import com.campuslink.entity.SocialNotificationEntity;
import java.util.List;
import java.util.Optional;

public interface SocialNotificationRepository {

  SocialNotificationEntity create(
      String recipientId,
      String actorId,
      String targetId,
      String type,
      String title,
      String body);

  List<SocialNotificationEntity> findForRecipient(String recipientId);

  Optional<SocialNotificationEntity> findByIdForRecipient(String recipientId, String notificationId);

  int countUnread(String recipientId);

  int markAllRead(String recipientId);

  int markRead(String recipientId, String notificationId);
}
