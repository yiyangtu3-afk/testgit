package com.campuslink.activity.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ActivityDtos {
  private ActivityDtos() {}
  public record CreateActivityRequest(@NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 2000) String description, @NotBlank @Size(max = 60) String category,
      @NotBlank @Size(max = 160) String location, @NotNull LocalDateTime startsAt,
      @NotNull LocalDateTime endsAt, @Min(1) @Max(10000) int capacity) {}
  public record ReviewActivityRequest(@NotBlank String decision, @Size(max = 500) String reason) {}
  public record ActivityView(String id, String title, String description, String category,
      String location, LocalDateTime startsAt, LocalDateTime endsAt, int capacity,
      String organizerId, String organizerName, String status, String reviewDecision,
      String reviewReason, String reviewerId, String reviewerName, LocalDateTime reviewedAt,
      LocalDateTime createdAt) {}
  public record RegistrationView(String id, String activityId, String status, int queuePosition,
      LocalDateTime registeredAt, LocalDateTime waitlistedAt) {}
  public record RosterEntryView(String registrationId, String attendeeId, String attendeeName,
      String status, int queuePosition, LocalDateTime registeredAt, LocalDateTime waitlistedAt,
      LocalDateTime checkedInAt) {}
  public record RosterView(String activityId, String title, int capacity, int registeredCount,
      int waitlistedCount, int checkedInCount, List<RosterEntryView> entries) {}
  public record ActivityMetricsView(int registrationCount, int checkedInCount) {}
  public record CheckInCredentialView(String activityId, String code) {}
  public record VerifyCheckInCredentialRequest(@NotBlank @Size(max = 128) String code) {}
}
