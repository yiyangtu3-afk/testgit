package com.campuslink.mapper;

import com.campuslink.entity.ActivityCheckInCredentialEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityCheckInCredentialMapper {

  @Select("""
      select id, registration_id as registrationId, token_hash as tokenHash, issued_at as issuedAt
      from activity_check_in_credentials
      where registration_id = #{registrationId}
      """)
  ActivityCheckInCredentialEntity findByRegistrationId(@Param("registrationId") String registrationId);

  @Select("""
      select id, registration_id as registrationId, token_hash as tokenHash, issued_at as issuedAt
      from activity_check_in_credentials
      where token_hash = #{tokenHash}
      for update
      """)
  ActivityCheckInCredentialEntity findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Insert("""
      insert into activity_check_in_credentials (id, registration_id, token_hash)
      values (#{id}, #{registrationId}, #{tokenHash})
      """)
  void insert(@Param("id") String id, @Param("registrationId") String registrationId,
      @Param("tokenHash") String tokenHash);

  @Update("""
      update activity_check_in_credentials
      set token_hash = #{tokenHash}, issued_at = current_timestamp(6)
      where id = #{credentialId}
      """)
  void replaceTokenHash(@Param("credentialId") String credentialId,
      @Param("tokenHash") String tokenHash);
}
