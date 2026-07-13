package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.ActivityService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = CampusLinkApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.sql.init.mode=never")
@Transactional
@Rollback
class ActivityRegistrationRepositoryIntegrationTest {
  @Autowired ActivityService activities;
  @Autowired ActivityRegistrationService registrations;
  @Autowired ActivityRegistrationRepository repository;
  @Autowired UserRepository users;

  @Test void registrationAndPromotionHistoryRollBackWithTestTransaction() {
    var teacher = users.findById("u-2001").orElseThrow();
    var admin = users.findById("u-2003").orElseThrow();
    var first = users.findById("u-1001").orElseThrow();
    var second = users.findById("u-2002").orElseThrow();
    var pending = activities.create(teacher, new CreateActivityRequest("报名事务测试", "验证递补历史",
        "测试", "T201", LocalDateTime.of(2026, 9, 3, 9, 0),
        LocalDateTime.of(2026, 9, 3, 11, 0), 1));
    var published = activities.review(admin, pending.id(), new ReviewActivityRequest("approve", null));

    assertThat(registrations.register(first, published.id()).status()).isEqualTo("registered");
    assertThat(registrations.register(second, published.id()).status()).isEqualTo("waitlisted");
    registrations.cancel(first, published.id());

    assertThat(repository.find(published.id(), second.id()).status()).isEqualTo("registered");
    assertThat(repository.findEvents(published.id())).extracting("eventType")
        .containsExactly("registered", "waitlisted", "cancelled", "promoted");
  }
}
