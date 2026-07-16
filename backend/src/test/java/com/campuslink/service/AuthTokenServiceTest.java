package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

  private final InMemoryAuthSessionRepository authSessions = new InMemoryAuthSessionRepository();
  private final AuthTokenService authTokenService = new AuthTokenService(
      authSessions,
      new InMemoryUserRepository(List.of(
          new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
          new UserEntity("u-2003", "教务管理员", "管理员账号", "13800000004", "online"))));

  @Test
  void issuedTokenCanBeResolvedFromSessionRepository() {
    String token = authTokenService.issueToken("u-1001");

    assertThat(authSessions.findUserIdByToken(token)).contains("u-1001");
    assertThat(authTokenService.requireUserId("Bearer " + token)).isEqualTo("u-1001");
  }

  @Test
  void missingSessionIsRejected() {
    JwtTokenCodec issuer = new JwtTokenCodec(
        "campuslink-test-signing-secret-must-have-32-bytes", 3600, Clock.systemUTC());

    assertThatThrownBy(() -> authTokenService.requireUserId("Bearer " + issuer.issue("u-1001")))
        .isInstanceOf(SecurityException.class)
        .hasMessage("登录已失效，请重新登录");
  }

  @Test
  void adminRequirementUsesPersistedSessionUser() {
    String token = authTokenService.issueToken("u-2003");

    assertThat(authTokenService.requireAdmin("Bearer " + token).id()).isEqualTo("u-2003");
  }

  @Test
  void expiredOrLoggedOutJwtIsRejected() {
    JwtTokenCodec issuer = new JwtTokenCodec(
        "campuslink-test-signing-secret-must-have-32-bytes", 1,
        Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));
    AuthTokenService expiredTokens = new AuthTokenService(
        authSessions,
        new InMemoryUserRepository(List.of(new UserEntity(
            "u-1001", "林一", "学生账号", "13800000001", "online"))),
        new JwtTokenCodec(
            "campuslink-test-signing-secret-must-have-32-bytes", 1,
            Clock.fixed(Instant.parse("2026-07-15T00:00:02Z"), ZoneOffset.UTC)));
    String expired = issuer.issue("u-1001");
    authSessions.save(expired, "u-1001");

    assertThatThrownBy(() -> expiredTokens.requireUserId("Bearer " + expired))
        .isInstanceOf(SecurityException.class)
        .hasMessage("登录已过期，请重新登录");

    String token = authTokenService.issueToken("u-1001");
    authTokenService.logout("Bearer " + token);
    assertThatThrownBy(() -> authTokenService.requireUserId("Bearer " + token))
        .isInstanceOf(SecurityException.class)
        .hasMessage("登录已失效，请重新登录");
  }

  private static final class InMemoryAuthSessionRepository implements AuthSessionRepository {

    private final Map<String, String> tokenUsers = new HashMap<>();

    @Override
    public void save(String token, String userId) {
      tokenUsers.put(token, userId);
    }

    @Override
    public Optional<String> findUserIdByToken(String token) {
      return Optional.ofNullable(tokenUsers.get(token));
    }

    @Override
    public void delete(String token) {
      tokenUsers.remove(token);
    }
  }

  private static final class InMemoryUserRepository implements UserRepository {

    private final List<UserEntity> users;

    private InMemoryUserRepository(List<UserEntity> users) {
      this.users = users;
    }

    @Override
    public List<UserEntity> findAll() {
      return users;
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
      return users.stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public Optional<UserEntity> findByPhone(String phone) {
      return users.stream().filter(user -> user.phone().equals(phone)).findFirst();
    }

    @Override
    public void updatePresence(String userId, String presence) {
    }
  }
}
