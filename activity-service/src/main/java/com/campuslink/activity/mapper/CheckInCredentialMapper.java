package com.campuslink.activity.mapper;

import com.campuslink.activity.domain.CheckInCredentialRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CheckInCredentialMapper {
  @Select("select id,registration_id as registrationId,token_hash as tokenHash from activity_check_in_credentials where registration_id=#{registrationId}") CheckInCredentialRecord byRegistration(@Param("registrationId") String registrationId);
  @Select("select id,registration_id as registrationId,token_hash as tokenHash from activity_check_in_credentials where token_hash=#{hash} for update") CheckInCredentialRecord byHashForUpdate(@Param("hash") String hash);
  @Insert("insert into activity_check_in_credentials (id,registration_id,token_hash) values (#{id},#{registrationId},#{hash})") void insert(@Param("id") String id, @Param("registrationId") String registrationId, @Param("hash") String hash);
  @Update("update activity_check_in_credentials set token_hash=#{hash},issued_at=current_timestamp(6) where id=#{id}") void replace(@Param("id") String id, @Param("hash") String hash);
}
