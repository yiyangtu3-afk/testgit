package com.campuslink.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ActivityRegistrationDtos {

  private ActivityRegistrationDtos() {
  }

  public record RegistrationView(
      String id,
      String activityId,
      String status,
      int queuePosition,
      LocalDateTime registeredAt,
      LocalDateTime waitlistedAt) {
  }

  public record RosterEntryView(
      String registrationId,
      String attendeeId,
      String attendeeName,
      String status,
      int queuePosition,
      LocalDateTime registeredAt,
      LocalDateTime waitlistedAt,
      LocalDateTime checkedInAt) {
  }

  public record RosterView(
      String activityId,
      String title,
      int capacity,
      int registeredCount,
      int waitlistedCount,
      int checkedInCount,
      List<RosterEntryView> entries) {
  }

  public record ActivityMetricsView(int registrationCount, int checkedInCount) {
  }
}
