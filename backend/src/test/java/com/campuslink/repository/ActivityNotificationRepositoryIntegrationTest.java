package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.ActivityService;
import java.time.LocalDateTime;
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
class ActivityNotificationRepositoryIntegrationTest {

  @Autowired ActivityService activities;
  @Autowired ActivityRegistrationService registrations;
  @Autowired ActivityNotificationService notifications;
  @Autowired ActivityNotificationRepository repository;
  @Autowired UserRepository users;

  @Test
  void reviewRegistrationWaitlistAndPromotionNotificationsRollBackTogether() {
    var teacher = users.findById("u-2001").orElseThrow();
    var admin = users.findById("u-2003").orElseThrow();
    var firstStudent = users.findById("u-1001").orElseThrow();
    var waitlistedStudent = users.findById("u-2002").orElseThrow();
    var pending = activities.create(teacher, new CreateActivityRequest(
        "通知事务测试", "验证通知和活动业务同事务回滚", "测试", "T202",
        LocalDateTime.of(2026, 9, 8, 9, 0),
        LocalDateTime.of(2026, 9, 8, 11, 0), 1));
    var published = activities.review(
        admin, pending.id(), new ReviewActivityRequest("approve", null));

    registrations.register(firstStudent, published.id());
    registrations.register(waitlistedStudent, published.id());
    registrations.cancel(firstStudent, published.id());

    assertThat(repository.findForRecipient(teacher.id()).stream()
        .filter(notification -> notification.activityId().equals(published.id())))
        .extracting("type")
        .containsExactly("activity.review.approved");
    assertThat(repository.findForRecipient(waitlistedStudent.id()).stream()
        .filter(notification -> notification.activityId().equals(published.id())))
        .extracting("type")
        .containsExactly(
            "activity.registration.promoted",
            "activity.registration.waitlisted");

    notifications.markAllRead(waitlistedStudent);
    assertThat(repository.findForRecipient(waitlistedStudent.id()).stream()
        .filter(notification -> notification.activityId().equals(published.id())))
        .allMatch(notification -> notification.readAt() != null);
  }
}
