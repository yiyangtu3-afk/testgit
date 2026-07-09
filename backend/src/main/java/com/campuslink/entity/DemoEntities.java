package com.campuslink.entity;

import java.util.List;

public final class DemoEntities {

  private DemoEntities() {
  }

  public record UserEntity(String id, String name, String role, String phone, String status) {
  }

  public record AttachmentEntity(String id, String name, long size, String type, String kind) {
  }

  public record MessageEntity(
      Long id,
      String from,
      String text,
      String time,
      boolean deleted,
      List<AttachmentEntity> attachments) {
  }

  public record PostEntity(
      Long id,
      String author,
      String body,
      String visibility,
      int likes,
      int comments,
      String moderationStatus,
      String moderationReason) {
  }

  public record CommentEntity(Long id, String author, String body, String time, String moderationStatus) {
  }

  public record FriendRequestEntity(
      String id,
      String fromUserId,
      String toUserId,
      String status) {
  }

  public record ModerationItemEntity(
      String id,
      String type,
      Long targetId,
      Long postId,
      String title,
      String author,
      String body,
      String status,
      String reason,
      String submittedAt,
      String time) {
  }

  public record AuditEventEntity(String id, String time, String module, String event) {
  }
}
