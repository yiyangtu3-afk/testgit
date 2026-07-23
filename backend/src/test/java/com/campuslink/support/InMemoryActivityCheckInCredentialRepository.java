package com.campuslink.support;

import com.campuslink.entity.ActivityCheckInCredentialEntity;
import com.campuslink.repository.ActivityCheckInCredentialRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryActivityCheckInCredentialRepository
    implements ActivityCheckInCredentialRepository {
  private final List<ActivityCheckInCredentialEntity> credentials = new ArrayList<>();

  @Override public ActivityCheckInCredentialEntity findByRegistrationId(String registrationId) {
    return credentials.stream().filter(item -> item.registrationId().equals(registrationId))
        .findFirst().orElse(null);
  }

  @Override public ActivityCheckInCredentialEntity findByTokenHashForUpdate(String tokenHash) {
    return credentials.stream().filter(item -> item.tokenHash().equals(tokenHash))
        .findFirst().orElse(null);
  }

  @Override public ActivityCheckInCredentialEntity create(String registrationId, String tokenHash) {
    var credential = new ActivityCheckInCredentialEntity("credential-" + (credentials.size() + 1),
        registrationId, tokenHash, LocalDateTime.of(2026, 7, 23, 9, credentials.size()));
    credentials.add(credential);
    return credential;
  }

  @Override public void replaceTokenHash(String credentialId, String tokenHash) {
    for (int index = 0; index < credentials.size(); index++) {
      var current = credentials.get(index);
      if (current.id().equals(credentialId)) {
        credentials.set(index, new ActivityCheckInCredentialEntity(current.id(),
            current.registrationId(), tokenHash, LocalDateTime.of(2026, 7, 23, 10, index)));
        return;
      }
    }
  }
}
