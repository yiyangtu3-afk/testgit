package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryActivityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityRegistrationServiceTest {
  private final InMemoryActivityRepository activities = new InMemoryActivityRepository();
  private final InMemoryActivityRegistrationRepository registrations =
      new InMemoryActivityRegistrationRepository();
  private final InMemoryActivityNotificationRepository notificationRepository =
      new InMemoryActivityNotificationRepository();
  private final ActivityNotificationService notifications =
      new ActivityNotificationService(notificationRepository);
  private final ActivityRegistrationService service =
      new ActivityRegistrationService(activities, registrations, notifications);
  private String activityId;

  @BeforeEach void publishCapacityOneActivity() {
    var activityService = new ActivityService(activities,
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var teacher = user("u-teacher", "教师");
    var admin = user("u-admin", "管理员");
    var pending = activityService.create(teacher, new CreateActivityRequest("测试活动", "测试报名",
        "科技", "A201", LocalDateTime.of(2026, 8, 1, 9, 0),
        LocalDateTime.of(2026, 8, 1, 11, 0), 1));
    activityId = activityService.review(admin, pending.id(),
        new ReviewActivityRequest("approve", null)).id();
  }

  @Test void firstStudentRegistersAndNextStudentWaitlists() {
    var firstStudent = user("u-one", "学生");
    var secondStudent = user("u-two", "学生");
    assertThat(service.register(firstStudent, activityId).status()).isEqualTo("registered");
    var waitlisted = service.register(secondStudent, activityId);
    assertThat(waitlisted.status()).isEqualTo("waitlisted");
    assertThat(waitlisted.queuePosition()).isEqualTo(1);
    assertThat(activities.findById(activityId).orElseThrow().status()).isEqualTo("full");
    assertThat(notifications.summary(firstStudent).items())
        .singleElement()
        .extracting("type", "title")
        .containsExactly("activity.registration.registered", "活动报名成功");
    assertThat(notifications.summary(secondStudent).items())
        .singleElement()
        .satisfies(notification -> {
          assertThat(notification.type()).isEqualTo("activity.registration.waitlisted");
          assertThat(notification.title()).isEqualTo("已加入活动候补");
          assertThat(notification.body()).contains("第 1 位");
        });
  }

  @Test void cancellationPromotesOldestWaitlistedStudent() {
    var firstStudent = user("u-one", "学生");
    var waitlistedStudent = user("u-two", "学生");
    service.register(firstStudent, activityId);
    service.register(waitlistedStudent, activityId);
    service.cancel(firstStudent, activityId);
    assertThat(registrations.find(activityId, "u-two").status()).isEqualTo("registered");
    assertThat(registrations.findEvents(activityId)).extracting("eventType")
        .containsExactly("registered", "waitlisted", "cancelled", "promoted");
    assertThat(notifications.summary(waitlistedStudent).items().getFirst())
        .satisfies(notification -> {
          assertThat(notification.type()).isEqualTo("activity.registration.promoted");
          assertThat(notification.title()).isEqualTo("候补已递补");
          assertThat(notification.body()).contains("已获得活动名额");
        });
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
