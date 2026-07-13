package com.campuslink.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ActivityNotificationDtos {

  private ActivityNotificationDtos() {
  }

  public record NotificationView(
      String id,
      String activityId,
      String type,
      String title,
      String body,
      boolean read,
      LocalDateTime createdAt) {
  }

  public record NotificationSummary(List<NotificationView> items, int unreadCount) {
  }
}
