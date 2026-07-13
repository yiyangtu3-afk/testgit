package com.campuslink.controller;

import com.campuslink.dto.ActivityNotificationDtos.NotificationSummary;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.AuthTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-notifications")
public class ActivityNotificationController {

  private final ActivityNotificationService notifications;
  private final AuthTokenService authTokens;

  public ActivityNotificationController(
      ActivityNotificationService notifications, AuthTokenService authTokens) {
    this.notifications = notifications;
    this.authTokens = authTokens;
  }

  @GetMapping
  public NotificationSummary summary(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.summary(authTokens.requireUser(authorization));
  }

  @PostMapping("/read-all")
  public NotificationSummary markAllRead(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.markAllRead(authTokens.requireUser(authorization));
  }
}
