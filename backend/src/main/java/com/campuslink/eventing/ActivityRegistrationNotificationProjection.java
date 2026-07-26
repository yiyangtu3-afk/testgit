package com.campuslink.eventing;

import com.campuslink.repository.ActivityRegistrationRepository;
import com.campuslink.repository.ActivityRepository;
import com.campuslink.service.ActivityNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityRegistrationNotificationProjection {

  public static final String CONSUMER_NAME = "campuslink-activity-notification-v1";

  private final ActivityEventReceiptService receipts;
  private final ActivityNotificationService notifications;
  private final ActivityRepository activities;
  private final ActivityRegistrationRepository registrations;

  public ActivityRegistrationNotificationProjection(
      ActivityEventReceiptService receipts,
      ActivityNotificationService notifications,
      ActivityRepository activities,
      ActivityRegistrationRepository registrations) {
    this.receipts = receipts;
    this.notifications = notifications;
    this.activities = activities;
    this.registrations = registrations;
  }

  @Transactional
  public void project(ActivityRegistrationMessage event) {
    if (!isNotificationEvent(event.eventType())
        || !receipts.recordIfFirst(CONSUMER_NAME, event.eventId())) {
      return;
    }
    String activityTitle = activityTitle(event);
    if ("activity.registration.promoted.v1".equals(event.eventType())) {
      notifications.recordPromotion(event.activityId(), activityTitle, event.attendeeId());
      return;
    }
    notifications.recordRegistrationResult(event.activityId(), activityTitle, event.attendeeId(),
        event.toStatus(), queuePosition(event));
  }

  private boolean isNotificationEvent(String eventType) {
    return "activity.registration.registered.v1".equals(eventType)
        || "activity.registration.waitlisted.v1".equals(eventType)
        || "activity.registration.promoted.v1".equals(eventType);
  }

  private String activityTitle(ActivityRegistrationMessage event) {
    if (event.activityTitle() != null && !event.activityTitle().isBlank()) {
      return event.activityTitle();
    }
    return activities.findById(event.activityId())
        .map(activity -> activity.title())
        .orElse("该活动");
  }

  private int queuePosition(ActivityRegistrationMessage event) {
    if (event.queuePosition() != null) {
      return event.queuePosition();
    }
    return "waitlisted".equals(event.toStatus())
        ? registrations.queuePosition(event.registrationId()) : 0;
  }
}
