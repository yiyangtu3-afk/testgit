package com.campuslink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatabaseHealthMapper {

  @Select("select database()")
  String databaseName();

  @Select("select count(*) from users")
  int countUsers();
}
