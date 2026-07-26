package com.campuslink.notification.api;

import com.campuslink.notification.service.NotificationAuthTokenService;
import com.campuslink.notification.service.NotificationService;
import com.campuslink.notification.service.NotificationService.NotificationSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-notifications")
public class ActivityNotificationController {
  private final NotificationService notifications;
  private final NotificationAuthTokenService tokens;

  public ActivityNotificationController(NotificationService notifications, NotificationAuthTokenService tokens) {
    this.notifications = notifications;
    this.tokens = tokens;
  }

  @GetMapping
  NotificationSummary summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.summary(tokens.requireUserId(authorization));
  }

  @PostMapping("/read-all")
  NotificationSummary markAllRead(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.markAllRead(tokens.requireUserId(authorization));
  }

  @PostMapping("/{notificationId}/read")
  NotificationSummary markRead(@PathVariable String notificationId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.markRead(tokens.requireUserId(authorization), notificationId);
  }
}
