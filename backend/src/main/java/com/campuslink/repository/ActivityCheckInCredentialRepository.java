package com.campuslink.repository;

import com.campuslink.entity.ActivityCheckInCredentialEntity;

public interface ActivityCheckInCredentialRepository {

  ActivityCheckInCredentialEntity findByRegistrationId(String registrationId);

  ActivityCheckInCredentialEntity findByTokenHashForUpdate(String tokenHash);

  ActivityCheckInCredentialEntity create(String registrationId, String tokenHash);

  void replaceTokenHash(String credentialId, String tokenHash);
}
