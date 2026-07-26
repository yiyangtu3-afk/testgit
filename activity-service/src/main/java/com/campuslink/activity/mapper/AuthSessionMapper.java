package com.campuslink.activity.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthSessionMapper {
  @Select("select user_id from auth_sessions where token=#{token}") String userId(@Param("token") String token);
}
