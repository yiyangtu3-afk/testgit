package com.campuslink.repository;

import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.mapper.SocialNotificationMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSocialNotificationRepository implements SocialNotificationRepository {

  private final SocialNotificationMapper mapper;

  public MyBatisSocialNotificationRepository(SocialNotificationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public SocialNotificationEntity create(
      String recipientId,
      String actorId,
      String targetId,
      String type,
      String title,
      String body) {
    String id = UUID.randomUUID().toString().replace("-", "");
    mapper.insert(id, recipientId, actorId, targetId, type, title, body);
    return mapper.findById(id);
  }

  @Override
  public List<SocialNotificationEntity> findForRecipient(String recipientId) {
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
}
