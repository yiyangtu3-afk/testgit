package com.campuslink.repository;

import com.campuslink.entity.SocialNotificationEntity;
import java.util.List;

public interface SocialNotificationRepository {

  SocialNotificationEntity create(
      String recipientId,
      String actorId,
      String targetId,
      String type,
      String title,
      String body);

  List<SocialNotificationEntity> findForRecipient(String recipientId);

  int countUnread(String recipientId);

  int markAllRead(String recipientId);
}
