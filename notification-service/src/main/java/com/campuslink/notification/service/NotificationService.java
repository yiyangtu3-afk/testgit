package com.campuslink.notification.service;

import com.campuslink.notification.domain.ActivityNotification;
import com.campuslink.notification.eventing.ActivityNotificationDeliveryMessage;
import com.campuslink.notification.eventing.ActivityRegistrationMessage;
import com.campuslink.notification.eventing.ActivityNotificationProjection;
import com.campuslink.notification.mapper.ActivityNotificationMapper;
import com.campuslink.notification.mapper.EventReceiptMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final ActivityNotificationMapper notifications;
  private final EventReceiptMapper receipts;
  private final KafkaTemplate<String, ActivityNotificationDeliveryMessage> delivery;
  private final String deliveryTopic;

  public NotificationService(ActivityNotificationMapper notifications, EventReceiptMapper receipts,
      KafkaTemplate<String, ActivityNotificationDeliveryMessage> delivery,
      @Value("${campuslink.eventing.activity-notification-delivery-topic}") String deliveryTopic) {
    this.notifications = notifications;
    this.receipts = receipts;
    this.delivery = delivery;
    this.deliveryTopic = deliveryTopic;
  }

  public NotificationSummary summary(String recipientId) {
    return new NotificationSummary(notifications.findForRecipient(recipientId).stream().map(this::view).toList(),
        notifications.countUnread(recipientId));
  }

  @Transactional
  public NotificationSummary markAllRead(String recipientId) {
    notifications.markAllRead(recipientId);
    return summary(recipientId);
  }

  @Transactional
  public NotificationSummary markRead(String recipientId, String notificationId) {
    notifications.markRead(recipientId, notificationId);
    return summary(recipientId);
  }

  @Transactional
  public void project(ActivityRegistrationMessage event) {
    if (!isNotificationEvent(event.eventType()) || receipts.insertIfAbsent(
        ActivityNotificationProjection.CONSUMER_NAME, event.eventId()) != 1) return;

    ActivityNotification saved = create(event);
    try {
      delivery.send(deliveryTopic, event.attendeeId(), new ActivityNotificationDeliveryMessage(
          saved.recipientId(), saved.id(), saved.activityId(), saved.type(), saved.title(), saved.body(),
          saved.createdAt()));
    } catch (RuntimeException ignored) {
      // Notification persistence is authoritative. A missed transient WebSocket wake-up recovers on next API load.
    }
  }

  private ActivityNotification create(ActivityRegistrationMessage event) {
    String title = event.activityTitle() == null || event.activityTitle().isBlank() ? "该活动" : event.activityTitle();
    String type;
    String heading;
    String body;
    if ("activity.review.approved.v1".equals(event.eventType())) {
      type = "activity.review.approved";
      heading = "活动已发布";
      body = "你提交的“" + title + "”已通过审核并公开发布。";
    } else if ("activity.review.rejected.v1".equals(event.eventType())) {
      type = "activity.review.rejected";
      heading = "活动审核未通过";
      String reason = event.toStatus() == null || event.toStatus().isBlank() ? "请修改后重新提交。" : "原因：" + event.toStatus();
      body = "你提交的“" + title + "”未通过审核。" + reason;
    } else if ("activity.registration.promoted.v1".equals(event.eventType())) {
      type = "activity.registration.promoted";
      heading = "候补已递补";
      body = "“" + title + "”已释放名额，你已获得活动名额。";
    } else if ("activity.registration.waitlisted.v1".equals(event.eventType())) {
      type = "activity.registration.waitlisted";
      heading = "已加入活动候补";
      int position = event.queuePosition() == null ? 0 : event.queuePosition();
      body = "“" + title + "”当前已满，你位于候补第 " + position + " 位。";
    } else {
      type = "activity.registration.registered";
      heading = "活动报名成功";
      body = "你已成功报名“" + title + "”。";
    }
    String id = UUID.randomUUID().toString().replace("-", "");
    notifications.insert(id, event.attendeeId(), event.activityId(), type, heading, body);
    return notifications.findById(id);
  }

  private boolean isNotificationEvent(String eventType) {
    return "activity.registration.registered.v1".equals(eventType)
        || "activity.registration.waitlisted.v1".equals(eventType)
        || "activity.registration.promoted.v1".equals(eventType)
        || "activity.review.approved.v1".equals(eventType)
        || "activity.review.rejected.v1".equals(eventType);
  }

  private NotificationView view(ActivityNotification notification) {
    return new NotificationView(notification.id(), notification.activityId(), notification.type(),
        notification.title(), notification.body(), notification.readAt() != null, notification.createdAt());
  }

  public record NotificationView(String id, String activityId, String type, String title, String body,
      boolean read, LocalDateTime createdAt) {}
  public record NotificationSummary(List<NotificationView> items, int unreadCount) {}
}
