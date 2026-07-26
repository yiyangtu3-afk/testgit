package com.campuslink.notification.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EventReceiptMapper {
  @Insert("insert ignore into event_processing_receipts (consumer_name, event_id) values (#{consumerName}, #{eventId})")
  int insertIfAbsent(@Param("consumerName") String consumerName, @Param("eventId") String eventId);
}
