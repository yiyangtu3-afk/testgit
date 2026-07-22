package com.campuslink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

public final class DemoDtos {

  private DemoDtos() {
  }

  public record PhoneRequest(
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone) {
  }

  public record LoginRequest(
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
      @NotBlank String code) {
  }

  public record RegisterRequest(
      @NotBlank(message = "姓名不能为空") @Size(max = 80, message = "姓名不能超过 80 个字符") String name,
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
      @NotBlank String code) {
  }

  public record DemoLoginRequest(@NotBlank String userId) {
  }

  public record SendMessageRequest(
      @NotBlank String text,
      List<@Valid AttachmentRequest> attachments) {
  }

  public record FriendRequest(String fromUserId, @NotBlank String userId) {
  }

  public record PresenceRequest(
      @Pattern(regexp = "^(online|invisible|offline)$", message = "在线状态不支持") String presence) {
  }

  public record PublishPostRequest(@NotBlank String body, @NotBlank String visibility) {
  }

  public record UpdatePersonalPostRequest(@NotBlank String body) {
  }

  public record PublishCommentRequest(@NotBlank String body) {
  }

  public record DeleteModerationRequest(List<String> itemIds) {
  }

  public record DeleteAuditEventsRequest(List<String> eventIds) {
  }

  public record ModerationDecisionRequest(
      @Size(max = 500, message = "审核意见不能超过 500 个字符") String comment) {
  }

  public record CodeResponse(String phone, String code) {
  }

  public record LoginResponse(String token, CurrentUser user) {
  }

  public record FriendRequestResponse(String userId, String status) {
  }

  public record FriendRequestView(
      String id,
      String fromUserId,
      String toUserId,
      String userId,
      String direction,
      String status,
      UserView user) {
  }

  public record PresenceResponse(String presence) {
  }

  public record CurrentUser(String id, String name, String role, String phone) {
  }

  public record UserView(String id, String name, String role, String phone, String status) {
  }

  public record AttachmentRequest(
      @NotBlank(message = "附件标识不能为空") @Size(max = 64, message = "附件标识过长") String id,
      @NotBlank(message = "附件名称不能为空") @Size(max = 255, message = "附件名称过长") String name,
      @PositiveOrZero(message = "附件大小不正确") long size,
      @NotBlank(message = "附件类型不能为空") @Size(max = 120, message = "附件类型过长") String type,
      @NotBlank(message = "附件类别不能为空") @Size(max = 40, message = "附件类别过长") String kind,
      @Size(max = 7_000_000, message = "图片数据过大") String dataUrl) {

    public AttachmentRequest(String id, String name, long size, String type, String kind) {
      this(id, name, size, type, kind, null);
    }
  }

  public record AttachmentView(
      String id,
      String name,
      long size,
      String type,
      String kind,
      boolean hasContent) {

    public AttachmentView(String id, String name, long size, String type, String kind) {
      this(id, name, size, type, kind, false);
    }
  }

  public record MessageView(
      Long id,
      String from,
      String text,
      String time,
      boolean deleted,
      List<AttachmentView> attachments) {
  }

  public record ConversationPageView(
      List<MessageView> messages,
      boolean hasMore,
      Long nextBeforeId) {
  }

  public record UnreadCountsView(Map<String, Integer> counts) {
  }

  public record ConversationPreviewsView(Map<String, MessageView> previews) {
  }

  public record PostView(
      Long id,
      String author,
      String body,
      String visibility,
      int likes,
      int comments,
      String moderationStatus,
      String moderationReason,
      boolean likedByCurrentUser) {
  }

  public record CommentView(Long id, String author, String body, String time, String moderationStatus) {
  }

  public record ModerationItemView(
      String id,
      String type,
      Long targetId,
      Long postId,
      String title,
      String author,
      String body,
      String status,
      String reason,
      String reviewerName,
      String reviewedAt,
      String reviewComment,
      String submittedAt,
      String time) {
  }

  public record ModerationAssistanceView(
      String suggestedDecision,
      String riskLevel,
      List<String> signals,
      String suggestedComment,
      String provider) {
  }

  public record AdminReportView(
      String generatedAt,
      String fileName,
      ReportRangeView range,
      Map<String, String> metrics,
      List<ModerationItemView> moderation,
      List<AuditEventView> auditEvents) {
  }

  public record ReportRangeView(String key, String label) {
  }

  public record DeleteModerationResponse(int deleted) {
  }

  public record DeleteAuditEventsResponse(int deleted) {
  }

  public record PersonalPostDeleteResponse(boolean deleted) {
  }

  public record AuditEventView(String id, String time, String module, String event) {
  }
}
