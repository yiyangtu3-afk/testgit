package com.campuslink.repository;

import com.campuslink.entity.ActivityNotificationEntity;
import java.util.List;

public interface ActivityNotificationRepository {

  ActivityNotificationEntity create(
      String recipientId, String activityId, String type, String title, String body);

  List<ActivityNotificationEntity> findForRecipient(String recipientId);

  int countUnread(String recipientId);

  int markAllRead(String recipientId);

  int markRead(String recipientId, String notificationId);
}
