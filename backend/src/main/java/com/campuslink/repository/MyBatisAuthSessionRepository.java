package com.campuslink.repository;

import com.campuslink.mapper.AuthSessionMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAuthSessionRepository implements AuthSessionRepository {

  private final AuthSessionMapper authSessionMapper;

  public MyBatisAuthSessionRepository(AuthSessionMapper authSessionMapper) {
    this.authSessionMapper = authSessionMapper;
  }

  @Override
  public void save(String token, String userId) {
    authSessionMapper.save(token, userId);
  }

  @Override
  public Optional<String> findUserIdByToken(String token) {
    return Optional.ofNullable(authSessionMapper.findUserIdByToken(token));
  }

  @Override
  public void delete(String token) {
    authSessionMapper.delete(token);
  }
}
