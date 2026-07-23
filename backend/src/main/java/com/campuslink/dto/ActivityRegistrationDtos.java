package com.campuslink.dto;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

  public record CheckInCredentialView(String activityId, String code) {
  }

  public record VerifyCheckInCredentialRequest(
      @NotBlank(message = "签到凭证不能为空")
      @Size(max = 128, message = "签到凭证格式不正确") String code) {
  }
}
