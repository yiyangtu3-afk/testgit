package com.campuslink.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class SocialNotificationDtos {

  private SocialNotificationDtos() {
  }

  public record NotificationView(
      String id,
      String targetId,
      String type,
      String title,
      String body,
      boolean read,
      LocalDateTime createdAt) {
  }

  public record NotificationSummary(List<NotificationView> items, int unreadCount) {
  }

  public record PostTarget(Long postId) {
  }
}
