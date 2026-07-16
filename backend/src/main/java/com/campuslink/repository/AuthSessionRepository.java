package com.campuslink.repository;

import java.util.Optional;

public interface AuthSessionRepository {

  void save(String token, String userId);

  Optional<String> findUserIdByToken(String token);

  default void delete(String token) {
  }
}
