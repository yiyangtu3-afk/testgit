package com.campuslink.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthSessionMapper {
  @Select("select user_id from auth_sessions where token = #{token}")
  String findUserIdByToken(@Param("token") String token);
}
