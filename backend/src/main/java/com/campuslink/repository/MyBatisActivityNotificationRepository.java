package com.campuslink.repository;

import com.campuslink.entity.ActivityNotificationEntity;
import com.campuslink.mapper.ActivityNotificationMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisActivityNotificationRepository implements ActivityNotificationRepository {

  private final ActivityNotificationMapper mapper;

  public MyBatisActivityNotificationRepository(ActivityNotificationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ActivityNotificationEntity create(
      String recipientId, String activityId, String type, String title, String body) {
    String id = UUID.randomUUID().toString().replace("-", "");
    mapper.insert(id, recipientId, activityId, type, title, body);
    return mapper.findById(id);
  }

  @Override
  public List<ActivityNotificationEntity> findForRecipient(String recipientId) {
    return mapper.findForRecipient(recipientId);
  }

  @Override
  public int countUnread(String recipientId) {
    return mapper.countUnread(recipientId);
  }

  @Override
  public int markAllRead(String recipientId) {
    return mapper.markAllRead(recipientId);
  }

  @Override
  public int markRead(String recipientId, String notificationId) {
    return mapper.markRead(recipientId, notificationId);
  }
}
