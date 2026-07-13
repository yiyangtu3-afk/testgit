package com.campuslink.dto;

import java.time.LocalDateTime;

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
}
