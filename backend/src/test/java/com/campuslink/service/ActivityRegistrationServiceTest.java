package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryActivityCheckInCredentialRepository;
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
      new ActivityRegistrationService(activities, registrations,
          new InMemoryActivityCheckInCredentialRepository(), notifications);
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

  @Test void organizerCanReadActiveRosterWithWaitlistPositions() {
    var teacher = user("u-teacher", "教师");
    service.register(user("u-one", "学生"), activityId);
    service.register(user("u-two", "学生"), activityId);

    var roster = service.roster(teacher, activityId);

    assertThat(roster.activityId()).isEqualTo(activityId);
    assertThat(roster.registeredCount()).isEqualTo(1);
    assertThat(roster.waitlistedCount()).isEqualTo(1);
    assertThat(roster.checkedInCount()).isZero();
    assertThat(roster.entries()).extracting("attendeeId", "status", "queuePosition")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("u-one", "registered", 0),
            org.assertj.core.groups.Tuple.tuple("u-two", "waitlisted", 1));
  }

  @Test void organizerChecksInRegisteredAttendeeAndRecordsEvent() {
    var teacher = user("u-teacher", "教师");
    service.register(user("u-one", "学生"), activityId);
    String registrationId = service.roster(teacher, activityId).entries().getFirst().registrationId();

    var checkedIn = service.checkIn(teacher, activityId, registrationId);

    assertThat(checkedIn.status()).isEqualTo("checked_in");
    assertThat(checkedIn.checkedInAt()).isNotNull();
    assertThat(service.roster(teacher, activityId).checkedInCount()).isEqualTo(1);
    assertThat(registrations.findEvents(activityId)).extracting("eventType")
        .containsExactly("registered", "checked_in");
  }

  @Test void organizerVerifiesOnlyTheLatestOpaqueCredentialForARegisteredAttendee() {
    var teacher = user("u-teacher", "教师");
    var student = user("u-one", "学生");
    service.register(student, activityId);
    String outdatedCode = service.credential(student, activityId).code();
    String activeCode = service.credential(student, activityId).code();

    assertThatThrownBy(() -> service.verifyCredential(teacher, activityId, outdatedCode))
        .isInstanceOf(ConflictException.class).hasMessage("签到凭证无效");

    var checkedIn = service.verifyCredential(teacher, activityId, activeCode);
    assertThat(checkedIn.attendeeId()).isEqualTo(student.id());
    assertThat(checkedIn.status()).isEqualTo("checked_in");
    assertThat(registrations.findEvents(activityId)).extracting("eventType")
        .containsExactly("registered", "checked_in");
  }

  @Test void administratorReadsRealRegistrationAndCheckInMetrics() {
    var teacher = user("u-teacher", "教师");
    service.register(user("u-one", "学生"), activityId);
    String registrationId = service.roster(teacher, activityId).entries().getFirst().registrationId();
    service.checkIn(teacher, activityId, registrationId);

    var metrics = service.metrics(user("u-admin", "管理员"));

    assertThat(metrics.registrationCount()).isEqualTo(1);
    assertThat(metrics.checkedInCount()).isEqualTo(1);
  }

  @Test void anotherOrganizerCannotReadOrCheckInTheRoster() {
    var owner = user("u-teacher", "教师");
    var otherTeacher = user("u-other-teacher", "教师");
    service.register(user("u-one", "学生"), activityId);
    String registrationId = service.roster(owner, activityId).entries().getFirst().registrationId();

    assertThatThrownBy(() -> service.roster(otherTeacher, activityId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("只能管理自己创建的活动");
    assertThatThrownBy(() -> service.checkIn(otherTeacher, activityId, registrationId))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("只能管理自己创建的活动");
  }

  @Test void organizerCannotCheckInAnActivityBeforeItIsApproved() {
    var teacher = user("u-teacher", "教师");
    var activityService = new ActivityService(activities,
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var pending = activityService.create(teacher, new CreateActivityRequest("待审签到测试", "测试审核边界",
        "科技", "A202", LocalDateTime.of(2026, 8, 2, 9, 0),
        LocalDateTime.of(2026, 8, 2, 11, 0), 1));

    assertThatThrownBy(() -> service.checkIn(teacher, pending.id(), "missing-registration"))
        .isInstanceOf(ConflictException.class)
        .hasMessage("当前活动暂不支持签到");
  }

  private UserEntity user(String id, String role) {
    return new UserEntity(id, id, role, "13800000000", "online");
  }
}
