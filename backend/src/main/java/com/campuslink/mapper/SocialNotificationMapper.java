package com.campuslink.mapper;

import com.campuslink.entity.SocialNotificationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SocialNotificationMapper {

  @Insert("""
      insert into social_notifications (
        id, recipient_id, actor_id, target_id, notification_type, title, body
      ) values (
        #{id}, #{recipientId}, #{actorId}, #{targetId}, #{type}, #{title}, #{body}
      )
      """)
  void insert(
      @Param("id") String id,
      @Param("recipientId") String recipientId,
      @Param("actorId") String actorId,
      @Param("targetId") String targetId,
      @Param("type") String type,
      @Param("title") String title,
      @Param("body") String body);

  @Select("""
      select id, recipient_id as recipientId, actor_id as actorId,
             target_id as targetId, notification_type as type, title, body,
             read_at as readAt, created_at as createdAt
      from social_notifications where id = #{id}
      """)
  SocialNotificationEntity findById(@Param("id") String id);

  @Select("""
      select id, recipient_id as recipientId, actor_id as actorId,
             target_id as targetId, notification_type as type, title, body,
             read_at as readAt, created_at as createdAt
      from social_notifications
      where recipient_id = #{recipientId}
      order by created_at desc, id desc
      limit 100
      """)
  List<SocialNotificationEntity> findForRecipient(@Param("recipientId") String recipientId);

  @Select("""
      select id, recipient_id as recipientId, actor_id as actorId,
             target_id as targetId, notification_type as type, title, body,
             read_at as readAt, created_at as createdAt
      from social_notifications
      where recipient_id = #{recipientId} and id = #{notificationId}
      """)
  SocialNotificationEntity findByIdForRecipient(
      @Param("recipientId") String recipientId,
      @Param("notificationId") String notificationId);

  @Select("""
      select count(*) from social_notifications
      where recipient_id = #{recipientId} and read_at is null
      """)
  int countUnread(@Param("recipientId") String recipientId);

  @Update("""
      update social_notifications set read_at = current_timestamp(6)
      where recipient_id = #{recipientId} and read_at is null
      """)
  int markAllRead(@Param("recipientId") String recipientId);

  @Update("""
      update social_notifications set read_at = current_timestamp(6)
      where id = #{notificationId} and recipient_id = #{recipientId} and read_at is null
      """)
  int markRead(
      @Param("recipientId") String recipientId,
      @Param("notificationId") String notificationId);
}
