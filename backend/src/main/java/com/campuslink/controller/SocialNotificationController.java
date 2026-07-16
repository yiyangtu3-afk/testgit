package com.campuslink.controller;

import com.campuslink.dto.SocialNotificationDtos.NotificationSummary;
import com.campuslink.dto.SocialNotificationDtos.PostTarget;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.SocialNotificationService;
import com.campuslink.service.SocialNotificationTargetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social-notifications")
public class SocialNotificationController {

  private final SocialNotificationService notifications;
  private final SocialNotificationTargetService targets;
  private final AuthTokenService authTokens;

  public SocialNotificationController(
      SocialNotificationService notifications,
      SocialNotificationTargetService targets,
      AuthTokenService authTokens) {
    this.notifications = notifications;
    this.targets = targets;
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

  @PostMapping("/{notificationId}/read")
  public NotificationSummary markRead(
      @PathVariable String notificationId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return notifications.markRead(authTokens.requireUserId(authorization), notificationId);
  }

  @GetMapping("/{notificationId}/post-target")
  public PostTarget postTarget(
      @PathVariable String notificationId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return targets.postTarget(authTokens.requireUserId(authorization), notificationId);
  }
}
