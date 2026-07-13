package com.campuslink.mapper;

import com.campuslink.entity.ActivityNotificationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityNotificationMapper {

  @Insert("""
      insert into activity_notifications (
        id, recipient_id, activity_id, notification_type, title, body
      ) values (
        #{id}, #{recipientId}, #{activityId}, #{type}, #{title}, #{body}
      )
      """)
  void insert(
      @Param("id") String id,
      @Param("recipientId") String recipientId,
      @Param("activityId") String activityId,
      @Param("type") String type,
      @Param("title") String title,
      @Param("body") String body);

  @Select("""
      select id, recipient_id as recipientId, activity_id as activityId,
             notification_type as type, title, body, read_at as readAt,
             created_at as createdAt
      from activity_notifications where id = #{id}
      """)
  ActivityNotificationEntity findById(@Param("id") String id);

  @Select("""
      select id, recipient_id as recipientId, activity_id as activityId,
             notification_type as type, title, body, read_at as readAt,
             created_at as createdAt
      from activity_notifications
      where recipient_id = #{recipientId}
      order by created_at desc, id desc
      limit 100
      """)
  List<ActivityNotificationEntity> findForRecipient(@Param("recipientId") String recipientId);

  @Select("""
      select count(*) from activity_notifications
      where recipient_id = #{recipientId} and read_at is null
      """)
  int countUnread(@Param("recipientId") String recipientId);

  @Update("""
      update activity_notifications set read_at = current_timestamp(6)
      where recipient_id = #{recipientId} and read_at is null
      """)
  int markAllRead(@Param("recipientId") String recipientId);
}
