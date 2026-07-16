package com.campuslink.support;

import com.campuslink.repository.AuthSessionRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAuthSessionRepository implements AuthSessionRepository {

  private final Map<String, String> sessions = new HashMap<>();

  @Override
  public void save(String token, String userId) {
    sessions.put(token, userId);
  }

  @Override
  public Optional<String> findUserIdByToken(String token) {
    return Optional.ofNullable(sessions.get(token));
  }

  @Override
  public void delete(String token) {
    sessions.remove(token);
  }
}
