package com.campuslink.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VerificationCodeMapper {

  @Insert("""
      insert into verification_codes (phone, code)
      values (#{phone}, #{code})
      on duplicate key update code = values(code)
      """)
  void save(@Param("phone") String phone, @Param("code") String code);

  @Select("select code from verification_codes where phone = #{phone}")
  String findByPhone(@Param("phone") String phone);
}
