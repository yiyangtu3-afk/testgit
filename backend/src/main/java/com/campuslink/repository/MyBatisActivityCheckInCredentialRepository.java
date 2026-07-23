package com.campuslink.repository;

import com.campuslink.entity.ActivityCheckInCredentialEntity;
import com.campuslink.mapper.ActivityCheckInCredentialMapper;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisActivityCheckInCredentialRepository implements ActivityCheckInCredentialRepository {

  private final ActivityCheckInCredentialMapper mapper;

  public MyBatisActivityCheckInCredentialRepository(ActivityCheckInCredentialMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ActivityCheckInCredentialEntity findByRegistrationId(String registrationId) {
    return mapper.findByRegistrationId(registrationId);
  }

  @Override
  public ActivityCheckInCredentialEntity findByTokenHashForUpdate(String tokenHash) {
    return mapper.findByTokenHashForUpdate(tokenHash);
  }

  @Override
  public ActivityCheckInCredentialEntity create(String registrationId, String tokenHash) {
    mapper.insert(newId(), registrationId, tokenHash);
    return mapper.findByRegistrationId(registrationId);
  }

  @Override
  public void replaceTokenHash(String credentialId, String tokenHash) {
    mapper.replaceTokenHash(credentialId, tokenHash);
  }

  private String newId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
