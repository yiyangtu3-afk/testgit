package com.campuslink.service;

import com.campuslink.dto.ActivityNotificationDtos.NotificationSummary;
import com.campuslink.dto.ActivityNotificationDtos.NotificationView;
import com.campuslink.entity.ActivityNotificationEntity;
import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ActivityNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityNotificationService {

  private final ActivityNotificationRepository notifications;
  private final ApplicationEventPublisher events;

  public ActivityNotificationService(ActivityNotificationRepository notifications) {
    this(notifications, ignored -> { });
  }

  @Autowired
  public ActivityNotificationService(
      ActivityNotificationRepository notifications,
      ApplicationEventPublisher events) {
    this.notifications = notifications;
    this.events = events;
  }

  public NotificationSummary summary(UserEntity recipient) {
    return new NotificationSummary(
        notifications.findForRecipient(recipient.id()).stream().map(this::view).toList(),
        notifications.countUnread(recipient.id()));
  }

  public void recordReviewResult(ActivityEntity activity) {
    if ("approved".equals(activity.reviewDecision())) {
      create(
          activity.organizerId(),
          activity.id(),
          "activity.review.approved",
          "活动已发布",
          "你提交的“" + activity.title() + "”已通过审核并公开发布。");
      return;
    }
    if ("rejected".equals(activity.reviewDecision())) {
      create(
          activity.organizerId(),
          activity.id(),
          "activity.review.rejected",
          "活动审核未通过",
          "你提交的“" + activity.title() + "”未通过审核。原因：" + activity.reviewReason());
    }
  }

  public void recordRegistrationResult(
      ActivityEntity activity, String attendeeId, String status, int queuePosition) {
    if ("registered".equals(status)) {
      create(
          attendeeId,
          activity.id(),
          "activity.registration.registered",
          "活动报名成功",
          "你已成功报名“" + activity.title() + "”。");
      return;
    }
    if ("waitlisted".equals(status)) {
      create(
          attendeeId,
          activity.id(),
          "activity.registration.waitlisted",
          "已加入活动候补",
          "“" + activity.title() + "”当前已满，你位于候补第 " + queuePosition + " 位。");
    }
  }

  public void recordPromotion(ActivityEntity activity, String attendeeId) {
    create(
        attendeeId,
        activity.id(),
        "activity.registration.promoted",
        "候补已递补",
        "“" + activity.title() + "”已释放名额，你已获得活动名额。");
  }

  @Transactional
  public NotificationSummary markAllRead(UserEntity recipient) {
    notifications.markAllRead(recipient.id());
    return summary(recipient);
  }

  @Transactional
  public NotificationSummary markRead(UserEntity recipient, String notificationId) {
    notifications.markRead(recipient.id(), notificationId);
    return summary(recipient);
  }

  private void create(
      String recipientId, String activityId, String type, String title, String body) {
    ActivityNotificationEntity notification = notifications.create(
        recipientId, activityId, type, title, body);
    events.publishEvent(new ActivityNotificationCreatedEvent(recipientId, view(notification)));
  }

  private NotificationView view(ActivityNotificationEntity notification) {
    return new NotificationView(
        notification.id(),
        notification.activityId(),
        notification.type(),
        notification.title(),
        notification.body(),
        notification.readAt() != null,
        notification.createdAt());
  }
}
