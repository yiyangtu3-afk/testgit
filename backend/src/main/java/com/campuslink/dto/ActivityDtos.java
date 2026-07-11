package com.campuslink.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ActivityDtos {

  private ActivityDtos() {
  }

  public record CreateActivityRequest(
      @NotBlank(message = "活动标题不能为空")
      @Size(max = 120, message = "活动标题不能超过120个字符")
      String title,
      @NotBlank(message = "活动详情不能为空")
      @Size(max = 2000, message = "活动详情不能超过2000个字符")
      String description,
      @NotBlank(message = "活动类别不能为空")
      @Size(max = 60, message = "活动类别不能超过60个字符")
      String category,
      @NotBlank(message = "活动地点不能为空")
      @Size(max = 160, message = "活动地点不能超过160个字符")
      String location,
      @NotNull(message = "活动开始时间不能为空") LocalDateTime startsAt,
      @NotNull(message = "活动结束时间不能为空") LocalDateTime endsAt,
      @Min(value = 1, message = "活动容量至少为1")
      @Max(value = 10000, message = "活动容量不能超过10000")
      int capacity) {
  }

  public record ReviewActivityRequest(
      @NotBlank(message = "审核动作不能为空") String decision,
      @Size(max = 500, message = "审核原因不能超过500个字符") String reason) {
  }

  public record ActivityView(
      String id,
      String title,
      String description,
      String category,
      String location,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      int capacity,
      String organizerId,
      String organizerName,
      String status,
      String reviewDecision,
      String reviewReason,
      String reviewerId,
      String reviewerName,
      LocalDateTime reviewedAt,
      LocalDateTime createdAt) {
  }
}
