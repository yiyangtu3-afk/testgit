package com.campuslink.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.activity.ActivityServiceApplication;
import com.campuslink.activity.api.ActivityDtos.RegistrationView;
import com.campuslink.activity.domain.UserDirectoryEntry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = ActivityServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:activity-registration-concurrency-schema.sql",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "campuslink.redis.registration-rate-limit.enabled=false",
        "campuslink.redis.registration-idempotency.enabled=false",
        "campuslink.redis.check-in-rate-limit.enabled=false"
    })
class ActivityRegistrationConcurrencyIntegrationTest {
  @Container
  static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("campuslink_activity_concurrency")
      .withUsername("campuslink")
      .withPassword("campuslink123");

  @DynamicPropertySource
  static void configureMySql(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
  }

  @Autowired private ActivityRegistrationApplicationService registrations;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void concurrentStudentsDoNotOversellAndDuplicatesRemainConflicts() throws Exception {
    String capacityActivity = "concurrency-capacity-1";
    insertPublishedActivity(capacityActivity, 2);

    List<Attempt> capacityAttempts = runTogether(List.of(
        student("student-1"), student("student-2"), student("student-3"),
        student("student-4"), student("student-5"), student("student-6")), capacityActivity);

    assertThat(capacityAttempts).extracting(Attempt::status)
        .containsExactlyInAnyOrder("registered", "registered", "waitlisted", "waitlisted",
            "waitlisted", "waitlisted");
    assertThat(count("select count(*) from activity_registrations where activity_id=? and status='registered'",
        capacityActivity)).isEqualTo(2);
    assertThat(count("select count(*) from activity_registrations where activity_id=? and status='waitlisted'",
        capacityActivity)).isEqualTo(4);
    assertThat(count("select count(*) from activity_registration_events where activity_id=?", capacityActivity))
        .isEqualTo(6);
    assertThat(count("select count(*) from outbox_events where aggregate_id=?", capacityActivity))
        .isEqualTo(6);
    assertThat(jdbc.queryForObject("select status from activities where id=?", String.class,
        capacityActivity)).isEqualTo("full");

    String duplicateActivity = "concurrency-duplicate-1";
    insertPublishedActivity(duplicateActivity, 2);
    List<Attempt> duplicateAttempts = runTogether(List.of(
        student("student-duplicate"), student("student-duplicate"), student("student-duplicate"),
        student("student-duplicate")), duplicateActivity);

    assertThat(duplicateAttempts).extracting(Attempt::status)
        .containsExactlyInAnyOrder("registered", "409", "409", "409");
    assertThat(count("select count(*) from activity_registrations where activity_id=?", duplicateActivity))
        .isEqualTo(1);
    assertThat(count("select count(*) from activity_registration_events where activity_id=?", duplicateActivity))
        .isEqualTo(1);
    assertThat(count("select count(*) from outbox_events where aggregate_id=?", duplicateActivity))
        .isEqualTo(1);
  }

  private List<Attempt> runTogether(List<UserDirectoryEntry> students, String activityId)
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(students.size());
    CountDownLatch ready = new CountDownLatch(students.size());
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Attempt>> futures = students.stream()
          .map(student -> executor.submit(() -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
              throw new AssertionError("并发报名测试未能同时开始");
            }
            try {
              RegistrationView result = registrations.register(student, activityId);
              return new Attempt(result.status());
            } catch (ResponseStatusException exception) {
              return new Attempt(Integer.toString(exception.getStatusCode().value()));
            }
          }))
          .toList();
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return futures.stream().map(this::result).toList();
    } finally {
      executor.shutdownNow();
    }
  }

  private Attempt result(Future<Attempt> future) {
    try {
      return future.get(15, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new AssertionError("并发报名测试未完成", exception);
    }
  }

  private void insertPublishedActivity(String id, int capacity) {
    jdbc.update("""
        insert into activities (id, title, description, category, location, starts_at, ends_at,
          capacity, organizer_id, status, review_decision)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'published', 'approved')
        """, id, "并发报名测试", "隔离 MySQL 并发测试", "技术", "T101",
        LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 1, 11, 0), capacity,
        "teacher-1");
  }

  private int count(String sql, String activityId) {
    return jdbc.queryForObject(sql, Integer.class, activityId);
  }

  private UserDirectoryEntry student(String id) {
    return new UserDirectoryEntry(id, id, "学生");
  }

  private record Attempt(String status) {}
}
