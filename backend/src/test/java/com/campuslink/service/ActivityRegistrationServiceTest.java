package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityRegistrationServiceTest {
  private final InMemoryActivityRepository activities = new InMemoryActivityRepository();
  private final InMemoryActivityRegistrationRepository registrations =
      new InMemoryActivityRegistrationRepository();
  private final ActivityRegistrationService service =
      new ActivityRegistrationService(activities, registrations);
  private String activityId;

  @BeforeEach void publishCapacityOneActivity() {
    var activityService = new ActivityService(activities);
    var teacher = user("u-teacher", "教师");
    var admin = user("u-admin", "管理员");
    var pending = activityService.create(teacher, new CreateActivityRequest("测试活动", "测试报名",
        "科技", "A201", LocalDateTime.of(2026, 8, 1, 9, 0),
        LocalDateTime.of(2026, 8, 1, 11, 0), 1));
    activityId = activityService.review(admin, pending.id(),
        new ReviewActivityRequest("approve", null)).id();
  }

  @Test void firstStudentRegistersAndNextStudentWaitlists() {
    assertThat(service.register(user("u-one", "学生"), activityId).status()).isEqualTo("registered");
    var waitlisted = service.register(user("u-two", "学生"), activityId);
    assertThat(waitlisted.status()).isEqualTo("waitlisted");
    assertThat(waitlisted.queuePosition()).isEqualTo(1);
    assertThat(activities.findById(activityId).orElseThrow().status()).isEqualTo("full");
  }

  @Test void cancellationPromotesOldestWaitlistedStudent() {
    service.register(user("u-one", "学生"), activityId);
    service.register(user("u-two", "学生"), activityId);
    service.cancel(user("u-one", "学生"), activityId);
    assertThat(registrations.find(activityId, "u-two").status()).isEqualTo("registered");
    assertThat(registrations.findEvents(activityId)).extracting("eventType")
        .containsExactly("registered", "waitlisted", "cancelled", "promoted");
  }

  @Test void duplicateRegistrationIsRejected() {
    var student = user("u-one", "学生");
    service.register(student, activityId);
    assertThatThrownBy(() -> service.register(student, activityId))
        .isInstanceOf(ConflictException.class).hasMessage("你已报名该活动");
  }

  private UserEntity user(String id, String role) {
    return new UserEntity(id, id, role, "13800000000", "online");
  }
}
