package com.campuslink.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthSessionMapper {

  @Insert("""
      insert into auth_sessions (token, user_id)
      values (#{token}, #{userId})
      on duplicate key update user_id = values(user_id)
      """)
  void save(@Param("token") String token, @Param("userId") String userId);

  @Select("""
      select user_id
      from auth_sessions
      where token = #{token}
      """)
  String findUserIdByToken(@Param("token") String token);
}
