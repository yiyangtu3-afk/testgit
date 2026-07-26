package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.EventProcessingReceiptRepository;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.ActivityService;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActivityRegistrationNotificationProjectionTest {

  @Test void projectsOnlyOneNotificationForDuplicateKafkaDelivery() {
    var notifications = new ActivityNotificationService(new InMemoryActivityNotificationRepository());
    var projection = projection(notifications, new InMemoryActivityRepository(),
        new InMemoryActivityRegistrationRepository());
    var event = message("event-1", "activity.registration.registered.v1", "registered",
        "校园编程赛", 0);

    projection.project(event);
    projection.project(event);

    assertThat(notifications.summary(student()).items()).singleElement().satisfies(notification -> {
      assertThat(notification.type()).isEqualTo("activity.registration.registered");
      assertThat(notification.body()).contains("校园编程赛");
    });
  }

  @Test void projectsWaitlistAndPromotionUsingEventContext() {
    var notifications = new ActivityNotificationService(new InMemoryActivityNotificationRepository());
    var projection = projection(notifications, new InMemoryActivityRepository(),
        new InMemoryActivityRegistrationRepository());

    projection.project(message("event-wait", "activity.registration.waitlisted.v1", "waitlisted",
        "校园编程赛", 3));
    projection.project(message("event-promote", "activity.registration.promoted.v1", "registered",
        "校园编程赛", null));

    assertThat(notifications.summary(student()).items()).extracting("type", "body").containsExactly(
        org.assertj.core.groups.Tuple.tuple("activity.registration.promoted", "“校园编程赛”已释放名额，你已获得活动名额。"),
        org.assertj.core.groups.Tuple.tuple("activity.registration.waitlisted", "“校园编程赛”当前已满，你位于候补第 3 位。"));
  }

  @Test void supportsPhaseOneMessagesByFallingBackToCurrentActivityAndQueueData() {
    var activities = new InMemoryActivityRepository();
    var registrations = new InMemoryActivityRegistrationRepository();
    String activityId = publishedActivity(activities);
    var registration = registrations.create(activityId, student().id(), "waitlisted");
    var notifications = new ActivityNotificationService(new InMemoryActivityNotificationRepository());
    var projection = projection(notifications, activities, registrations);
    var phaseOneMessage = new ActivityRegistrationMessage("event-old",
        "activity.registration.waitlisted.v1", registration.id(), activityId, student().id(), student().id(),
        null, "waitlisted", LocalDateTime.of(2026, 7, 25, 12, 0), null, null);

    projection.project(phaseOneMessage);

    assertThat(notifications.summary(student()).items()).singleElement().satisfies(notification ->
        assertThat(notification.body()).isEqualTo("“兼容活动”当前已满，你位于候补第 1 位。"));
  }

  private ActivityRegistrationNotificationProjection projection(
      ActivityNotificationService notifications,
      InMemoryActivityRepository activities,
      InMemoryActivityRegistrationRepository registrations) {
    return new ActivityRegistrationNotificationProjection(
        new ActivityEventReceiptService(new InMemoryReceiptRepository()), notifications, activities, registrations);
  }

  private ActivityRegistrationMessage message(
      String eventId, String eventType, String toStatus, String title, Integer queuePosition) {
    return new ActivityRegistrationMessage(eventId, eventType, "registration-1", "activity-1",
        student().id(), student().id(), null, toStatus, LocalDateTime.of(2026, 7, 25, 12, 0),
        title, queuePosition);
  }

  private String publishedActivity(InMemoryActivityRepository activities) {
    var service = new ActivityService(activities,
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var pending = service.create(new UserEntity("teacher", "教师", "教师", "1", "online"),
        new CreateActivityRequest("兼容活动", "说明", "科技", "A201",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 10, 0), 1));
    return service.review(new UserEntity("admin", "管理员", "管理员", "2", "online"), pending.id(),
        new ReviewActivityRequest("approve", null)).id();
  }

  private UserEntity student() {
    return new UserEntity("student-1", "学生", "学生", "3", "online");
  }

  private static final class InMemoryReceiptRepository implements EventProcessingReceiptRepository {
    private final Set<String> received = new HashSet<>();

    @Override public boolean recordIfFirst(String consumerName, String eventId) {
      return received.add(consumerName + ":" + eventId);
    }
  }
}
