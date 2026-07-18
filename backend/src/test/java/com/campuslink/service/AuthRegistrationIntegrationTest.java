package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.dto.DemoDtos.CodeResponse;
import com.campuslink.dto.DemoDtos.LoginResponse;
import com.campuslink.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.sql.init.mode=never")
@Transactional
@Rollback
class AuthRegistrationIntegrationTest {

  @Autowired AuthService authService;
  @Autowired UserRepository users;

  @Test
  void registrationPersistsStudentInMysqlWithinRollbackSafeTransaction() {
    String phone = "139" + String.format(
        "%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
    CodeResponse code = authService.createCode(phone);

    LoginResponse response = authService.register("注册验收同学", phone, code.code());

    assertThat(response.user().phone()).isEqualTo(phone);
    assertThat(response.user().role()).isEqualTo("学生账号");
    assertThat(users.findByPhone(phone)).hasValueSatisfying(user -> {
      assertThat(user.id()).isEqualTo(response.user().id());
      assertThat(user.name()).isEqualTo("注册验收同学");
      assertThat(user.status()).isEqualTo("online");
    });
  }
}
