package com.campuslink.mapper;

import com.campuslink.entity.OutboxEventEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxEventMapper {

  @Insert("""
      insert into outbox_events (
        id, aggregate_type, aggregate_id, event_type, payload, status, next_attempt_at
      ) values (
        #{id}, #{aggregateType}, #{aggregateId}, #{eventType}, #{payload}, 'pending', now(6)
      )
      """)
  void insert(@Param("id") String id, @Param("aggregateType") String aggregateType,
      @Param("aggregateId") String aggregateId, @Param("eventType") String eventType,
      @Param("payload") String payload);

  @Select("""
      select id, aggregate_type as aggregateType, aggregate_id as aggregateId,
             event_type as eventType, payload, status, attempts,
             next_attempt_at as nextAttemptAt, published_at as publishedAt,
             last_error as lastError, created_at as createdAt
      from outbox_events
      where status in ('pending', 'retry') and next_attempt_at <= now(6)
      order by created_at, id
      limit #{limit}
      """)
  List<OutboxEventEntity> findReady(@Param("limit") int limit);

  @Update("""
      update outbox_events
      set status = 'published', published_at = now(6), last_error = null
      where id = #{eventId} and status in ('pending', 'retry')
      """)
  int markPublished(@Param("eventId") String eventId);

  @Update("""
      update outbox_events
      set status = 'retry', attempts = attempts + 1,
          next_attempt_at = date_add(now(6), interval #{retryDelaySeconds} second),
          last_error = left(#{message}, 1000)
      where id = #{eventId} and status in ('pending', 'retry')
      """)
  int markRetry(@Param("eventId") String eventId, @Param("message") String message,
      @Param("retryDelaySeconds") int retryDelaySeconds);

  @Select("""
      select id, aggregate_type as aggregateType, aggregate_id as aggregateId,
             event_type as eventType, payload, status, attempts,
             next_attempt_at as nextAttemptAt, published_at as publishedAt,
             last_error as lastError, created_at as createdAt
      from outbox_events where id = #{eventId}
      """)
  OutboxEventEntity findById(@Param("eventId") String eventId);
}
