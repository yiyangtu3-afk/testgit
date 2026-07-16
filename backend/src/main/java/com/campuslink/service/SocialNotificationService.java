package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.NotificationSummary;
import com.campuslink.dto.SocialNotificationDtos.NotificationView;
import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.repository.SocialNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialNotificationService {

  private final SocialNotificationRepository notifications;

  public SocialNotificationService(SocialNotificationRepository notifications) {
    this.notifications = notifications;
  }

  public NotificationSummary summary(String recipientId) {
    return new NotificationSummary(
        notifications.findForRecipient(recipientId).stream().map(this::view).toList(),
        notifications.countUnread(recipientId));
  }

  public void recordPostLiked(
      String recipientId, String actorId, String actorName, Long postId) {
    notifications.create(
        recipientId,
        actorId,
        String.valueOf(postId),
        "social.post.liked",
        "动态收到新点赞",
        actorName + "赞了你的动态。");
  }

  public void recordPostCommented(
      String recipientId,
      String actorId,
      String actorName,
      Long commentId,
      String commentBody) {
    notifications.create(
        recipientId,
        actorId,
        String.valueOf(commentId),
        "social.post.commented",
        "动态收到新评论",
        actorName + "评论了你的动态：" + commentPreview(commentBody));
  }

  public void recordFriendRequestReceived(
      String recipientId, String actorId, String actorName, String requestId) {
    notifications.create(
        recipientId,
        actorId,
        requestId,
        "social.friend.requested",
        "新的好友申请",
        actorName + "向你发送了好友申请。");
  }

  public void recordFriendRequestResolved(
      String recipientId,
      String actorId,
      String actorName,
      String requestId,
      String status) {
    boolean accepted = "accepted".equals(status);
    notifications.create(
        recipientId,
        actorId,
        requestId,
        accepted ? "social.friend.accepted" : "social.friend.rejected",
        accepted ? "好友申请已同意" : "好友申请未通过",
        actorName + (accepted ? "已同意" : "拒绝") + "你的好友申请。");
  }

  @Transactional
  public NotificationSummary markAllRead(String recipientId) {
    notifications.markAllRead(recipientId);
    return summary(recipientId);
  }

  private NotificationView view(SocialNotificationEntity notification) {
    return new NotificationView(
        notification.id(),
        notification.targetId(),
        notification.type(),
        notification.title(),
        notification.body(),
        notification.readAt() != null,
        notification.createdAt());
  }

  private String commentPreview(String body) {
    String normalized = body.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "…";
  }
}
