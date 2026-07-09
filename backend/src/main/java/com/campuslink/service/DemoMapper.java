package com.campuslink.service;

import com.campuslink.dto.DemoDtos.AttachmentRequest;
import com.campuslink.dto.DemoDtos.AttachmentView;
import com.campuslink.dto.DemoDtos.AuditEventView;
import com.campuslink.dto.DemoDtos.CommentView;
import com.campuslink.dto.DemoDtos.FriendRequestView;
import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.ModerationItemView;
import com.campuslink.dto.DemoDtos.PostView;
import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.AuditEventEntity;
import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import java.util.List;

final class DemoMapper {

  private DemoMapper() {
  }

  static UserView toUserView(UserEntity user) {
    return new UserView(user.id(), user.name(), user.role(), user.phone(), user.status());
  }

  static AttachmentEntity toAttachmentEntity(AttachmentRequest attachment) {
    return new AttachmentEntity(
        attachment.id(),
        attachment.name(),
        attachment.size(),
        attachment.type(),
        attachment.kind());
  }

  static AttachmentView toAttachmentView(AttachmentEntity attachment) {
    return new AttachmentView(
        attachment.id(),
        attachment.name(),
        attachment.size(),
        attachment.type(),
        attachment.kind());
  }

  static MessageView toMessageView(MessageEntity message) {
    return new MessageView(
        message.id(),
        message.from(),
        message.text(),
        message.time(),
        message.deleted(),
        message.attachments().stream().map(DemoMapper::toAttachmentView).toList());
  }

  static PostView toPostView(PostEntity post) {
    return new PostView(
        post.id(),
        post.author(),
        post.body(),
        post.visibility(),
        post.likes(),
        post.comments(),
        post.moderationStatus(),
        post.moderationReason());
  }

  static CommentView toCommentView(CommentEntity comment) {
    return new CommentView(
        comment.id(),
        comment.author(),
        comment.body(),
        comment.time(),
        comment.moderationStatus());
  }

  static ModerationItemView toModerationItemView(ModerationItemEntity item) {
    return new ModerationItemView(
        item.id(),
        item.type(),
        item.targetId(),
        item.postId(),
        item.title(),
        item.author(),
        item.body(),
        item.status(),
        item.reason(),
        item.submittedAt(),
        item.time());
  }

  static AuditEventView toAuditEventView(AuditEventEntity event) {
    return new AuditEventView(event.id(), event.time(), event.module(), event.event());
  }

  static FriendRequestView toFriendRequestView(
      FriendRequestEntity request,
      String currentUserId,
      UserView user) {
    boolean incoming = request.toUserId().equals(currentUserId);
    return new FriendRequestView(
        request.id(),
        request.fromUserId(),
        request.toUserId(),
        incoming ? request.fromUserId() : request.toUserId(),
        incoming ? "incoming" : "outgoing",
        request.status(),
        user);
  }

  static List<AttachmentEntity> toAttachmentEntities(List<AttachmentRequest> attachments) {
    if (attachments == null) {
      return List.of();
    }
    return attachments.stream().map(DemoMapper::toAttachmentEntity).toList();
  }
}
