package com.campuslink.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.campuslink.notification.domain.ActivityNotification;
import com.campuslink.notification.eventing.ActivityNotificationDeliveryMessage;
import com.campuslink.notification.eventing.ActivityRegistrationMessage;
import com.campuslink.notification.mapper.ActivityNotificationMapper;
import com.campuslink.notification.mapper.EventReceiptMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class NotificationServiceTest {

  @Test
  void projectsEachKafkaEventOnlyOnceWithoutReadingActivityData() {
    var notifications = new RecordingNotificationMapper();
    var receipts = new RecordingReceiptMapper();
    var service = service(notifications, receipts);
    var event = event("event-1", "activity.registration.waitlisted.v1", "校园编程赛", 3);

    service.project(event);
    service.project(event);

    assertThat(notifications.items.values()).singleElement().satisfies(notification -> {
      assertThat(notification.type()).isEqualTo("activity.registration.waitlisted");
      assertThat(notification.body()).contains("校园编程赛").contains("第 3 位");
    });
  }

  @Test
  void preservesNotificationReadSummaryContract() {
    var notifications = new RecordingNotificationMapper();
    var service = service(notifications, new RecordingReceiptMapper());
    service.project(event("event-2", "activity.registration.registered.v1", "讲座", 0));
    String id = notifications.items.keySet().iterator().next();

    assertThat(service.summary("student-1").unreadCount()).isEqualTo(1);
    assertThat(service.markRead("student-1", id).unreadCount()).isZero();
    assertThat(service.summary("student-1").items()).singleElement().extracting("read").isEqualTo(true);
  }

  @Test
  void projectsReviewRejectionForTheActivityOrganizer() {
    var notifications = new RecordingNotificationMapper();
    var service = service(notifications, new RecordingReceiptMapper());

    service.project(new ActivityRegistrationMessage("event-review", "activity.review.rejected.v1",
        null, "activity-1", "teacher-1", "admin-1", "pending", "时间信息不完整",
        LocalDateTime.of(2026, 7, 26, 12, 0), "编程赛", null));

    assertThat(notifications.items.values()).singleElement().satisfies(notification -> {
      assertThat(notification.type()).isEqualTo("activity.review.rejected");
      assertThat(notification.body()).contains("编程赛").contains("时间信息不完整");
    });
  }

  @SuppressWarnings("unchecked")
  private NotificationService service(
      ActivityNotificationMapper notifications, EventReceiptMapper receipts) {
    return new NotificationService(notifications, receipts,
        mock(KafkaTemplate.class), "campuslink.activity.notification.delivery.v1");
  }

  private ActivityRegistrationMessage event(String id, String type, String title, Integer position) {
    return new ActivityRegistrationMessage(id, type, "registration-1", "activity-1", "student-1",
        "student-1", null, "waitlisted", LocalDateTime.of(2026, 7, 26, 12, 0), title, position);
  }

  private static final class RecordingReceiptMapper implements EventReceiptMapper {
    private final List<String> received = new ArrayList<>();
    @Override public int insertIfAbsent(String consumerName, String eventId) {
      String key = consumerName + ':' + eventId;
      if (received.contains(key)) return 0;
      received.add(key);
      return 1;
    }
  }

  private static final class RecordingNotificationMapper implements ActivityNotificationMapper {
    private final Map<String, ActivityNotification> items = new LinkedHashMap<>();

    @Override public void insert(String id, String recipientId, String activityId, String type,
        String title, String body) {
      items.put(id, new ActivityNotification(id, recipientId, activityId, type, title, body,
          null, LocalDateTime.of(2026, 7, 26, 12, 0)));
    }
    @Override public ActivityNotification findById(String id) { return items.get(id); }
    @Override public List<ActivityNotification> findForRecipient(String recipientId) {
      return items.values().stream().filter(item -> item.recipientId().equals(recipientId)).toList();
    }
    @Override public int countUnread(String recipientId) {
      return (int) findForRecipient(recipientId).stream().filter(item -> item.readAt() == null).count();
    }
    @Override public int markAllRead(String recipientId) {
      return mark(recipientId, null);
    }
    @Override public int markRead(String recipientId, String notificationId) {
      return mark(recipientId, notificationId);
    }
    private int mark(String recipientId, String notificationId) {
      int changed = 0;
      for (var entry : items.entrySet()) {
        ActivityNotification item = entry.getValue();
        if (item.recipientId().equals(recipientId) && item.readAt() == null
            && (notificationId == null || entry.getKey().equals(notificationId))) {
          entry.setValue(new ActivityNotification(item.id(), item.recipientId(), item.activityId(), item.type(),
              item.title(), item.body(), LocalDateTime.of(2026, 7, 26, 13, 0), item.createdAt()));
          changed++;
        }
      }
      return changed;
    }
  }
}
