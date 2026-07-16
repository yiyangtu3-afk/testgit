package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
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
class AuthSessionRepositoryIntegrationTest {

  @Autowired AuthSessionRepository sessions;

  @Test
  void removesTheExactSessionTokenWithoutPersistingTestData() {
    String token = "test-session-" + "x".repeat(180);
    sessions.save(token, "u-1001");

    assertThat(sessions.findUserIdByToken(token)).contains("u-1001");
    sessions.delete(token);
    assertThat(sessions.findUserIdByToken(token)).isEmpty();
  }
}
