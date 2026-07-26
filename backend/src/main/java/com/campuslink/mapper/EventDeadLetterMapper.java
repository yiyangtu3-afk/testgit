package com.campuslink.mapper;

import com.campuslink.entity.EventDeadLetterEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EventDeadLetterMapper {

  @Insert("""
      insert into event_dead_letters (
        id, consumer_name, event_id, event_type, event_key, original_topic, payload, failure_message, status
      ) values (
        #{id}, #{consumerName}, #{eventId}, #{eventType}, #{eventKey}, #{originalTopic},
        #{payload}, left(#{failureMessage}, 1000), 'dead_letter'
      ) on duplicate key update
        event_key = values(event_key), original_topic = values(original_topic), payload = values(payload),
        failure_message = values(failure_message), status = 'dead_letter',
        delivery_count = delivery_count + 1, dead_lettered_at = now(6), replayed_at = null
      """)
  void upsert(
      @Param("id") String id,
      @Param("consumerName") String consumerName,
      @Param("eventId") String eventId,
      @Param("eventType") String eventType,
      @Param("eventKey") String eventKey,
      @Param("originalTopic") String originalTopic,
      @Param("payload") String payload,
      @Param("failureMessage") String failureMessage);

  @Select("""
      select id, consumer_name as consumerName, event_id as eventId, event_type as eventType,
             event_key as eventKey, original_topic as originalTopic, cast(payload as char) as payload,
             failure_message as failureMessage, status, delivery_count as deliveryCount,
             dead_lettered_at as deadLetteredAt, replayed_at as replayedAt
      from event_dead_letters
      where status = 'dead_letter'
      order by dead_lettered_at desc, id
      limit #{limit}
      """)
  List<EventDeadLetterEntity> findDeadLetters(@Param("limit") int limit);

  @Select("""
      select id, consumer_name as consumerName, event_id as eventId, event_type as eventType,
             event_key as eventKey, original_topic as originalTopic, cast(payload as char) as payload,
             failure_message as failureMessage, status, delivery_count as deliveryCount,
             dead_lettered_at as deadLetteredAt, replayed_at as replayedAt
      from event_dead_letters where id = #{id}
      """)
  EventDeadLetterEntity findById(@Param("id") String id);

  @Update("""
      update event_dead_letters
      set status = 'replayed', replayed_at = now(6)
      where id = #{id} and status = 'dead_letter'
      """)
  int markReplayed(@Param("id") String id);

  @Select("select count(*) from event_dead_letters where status = 'dead_letter'")
  int countDeadLetters();
}
