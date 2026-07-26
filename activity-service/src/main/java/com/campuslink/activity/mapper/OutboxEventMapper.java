package com.campuslink.activity.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Transitional shared Outbox table; phase seven moves its publisher with activity ownership. */
@Mapper
public interface OutboxEventMapper {
  @Insert("insert into outbox_events (id,aggregate_type,aggregate_id,event_type,payload,status,next_attempt_at) values (#{id},#{aggregateType},#{aggregateId},#{eventType},#{payload},'pending',now(6))")
  void insert(@Param("id") String id, @Param("aggregateType") String aggregateType,
      @Param("aggregateId") String aggregateId, @Param("eventType") String eventType,
      @Param("payload") String payload);
}
