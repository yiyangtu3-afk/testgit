package com.campuslink.mapper;

import com.campuslink.entity.DemoEntities.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

  @Select("""
      select id, name, major as role, phone, presence as status
      from users
      order by created_at, id
      """)
  List<UserEntity> findAll();

  @Select("""
      select id, name, major as role, phone, presence as status
      from users
      where id = #{userId}
      """)
  UserEntity findById(@Param("userId") String userId);

  @Select("""
      select id, name, major as role, phone, presence as status
      from users
      where phone = #{phone}
      """)
  UserEntity findByPhone(@Param("phone") String phone);

  @Insert("""
      insert into users (id, name, phone, major, avatar, presence)
      values (#{id}, #{name}, #{phone}, #{role}, #{avatar}, #{status})
      """)
  void insert(
      @Param("id") String id,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("role") String role,
      @Param("avatar") String avatar,
      @Param("status") String status);

  @Update("update users set presence = #{presence} where id = #{userId}")
  void updatePresence(@Param("userId") String userId, @Param("presence") String presence);
}
