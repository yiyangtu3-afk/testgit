package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.DemoDtos.CodeResponse;
import com.campuslink.dto.DemoDtos.LoginResponse;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.repository.VerificationCodeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

  private final InMemoryVerificationCodeRepository verificationCodes = new InMemoryVerificationCodeRepository();
  private final InMemoryAuthSessionRepository authSessions = new InMemoryAuthSessionRepository();
  private final InMemoryUserRepository users = new InMemoryUserRepository(List.of(
      new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online")));
  private final AuthService authService = new AuthService(
      verificationCodes,
      users,
      new AuditService(new TestAuditRepository()),
      new AuthTokenService(authSessions, users));

  @Test
  void createCodePersistsVerificationCode() {
    CodeResponse response = authService.createCode("13800000001");

    assertThat(response.code()).hasSize(6);
    assertThat(verificationCodes.findByPhone("13800000001")).contains(response.code());
  }

  @Test
  void loginReturnsDatabaseUserForMatchingCode() {
    verificationCodes.save("13800000001", "123456");

    LoginResponse response = authService.login("13800000001", "123456");

    assertThat(response.user().id()).isEqualTo("u-1001");
    assertThat(response.user().name()).isEqualTo("林一");
    assertThat(response.user().role()).isEqualTo("学生账号");
    assertThat(response.token()).startsWith("eyJ");
    assertThat(authSessions.findUserIdByToken(response.token())).contains("u-1001");
  }

  @Test
  void demoLoginReturnsTokenForSelectedUser() {
    LoginResponse response = authService.demoLogin("u-1001");

    assertThat(response.user().id()).isEqualTo("u-1001");
    assertThat(response.token()).startsWith("eyJ");
    assertThat(authSessions.findUserIdByToken(response.token())).contains("u-1001");
  }

  @Test
  void loginRejectsWrongCode() {
    verificationCodes.save("13800000001", "123456");

    assertThatThrownBy(() -> authService.login("13800000001", "000000"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("验证码不正确");
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
  }

  private static final class InMemoryVerificationCodeRepository implements VerificationCodeRepository {

    private final Map<String, String> codes = new HashMap<>();

    @Override
    public void save(String phone, String code) {
      codes.put(phone, code);
    }

    @Override
    public Optional<String> findByPhone(String phone) {
      return Optional.ofNullable(codes.get(phone));
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
