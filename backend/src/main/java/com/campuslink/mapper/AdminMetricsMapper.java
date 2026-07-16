package com.campuslink.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMetricsMapper {

  @Select("select count(*) from users")
  int countUsers();

  @Select("select count(*) from messages where created_at >= #{startTime}")
  int countMessagesSince(@Param("startTime") LocalDateTime startTime);
}
