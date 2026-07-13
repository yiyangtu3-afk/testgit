package com.campuslink.controller;

import com.campuslink.dto.SocialNotificationDtos.NotificationSummary;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.SocialNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social-notifications")
public class SocialNotificationController {

  private final SocialNotificationService notifications;
  private final AuthTokenService authTokens;

  public SocialNotificationController(
      SocialNotificationService notifications, AuthTokenService authTokens) {
    this.notifications = notifications;
    this.authTokens = authTokens;
  }

  @GetMapping
  public NotificationSummary summary(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.summary(authTokens.requireUserId(authorization));
  }

  @PostMapping("/read-all")
  public NotificationSummary markAllRead(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.markAllRead(authTokens.requireUserId(authorization));
  }
}
