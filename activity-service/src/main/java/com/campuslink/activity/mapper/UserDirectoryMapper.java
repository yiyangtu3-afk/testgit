package com.campuslink.activity.mapper;

import com.campuslink.activity.domain.UserDirectoryEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDirectoryMapper {
  @Select("select id,name,major as role from users where id=#{id}")
  UserDirectoryEntry find(@Param("id") String id);
}
