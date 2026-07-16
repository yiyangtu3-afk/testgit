package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.PostTarget;
import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.SocialNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class SocialNotificationTargetService {

  private final SocialNotificationRepository notifications;
  private final FeedRepository feed;

  public SocialNotificationTargetService(
      SocialNotificationRepository notifications, FeedRepository feed) {
    this.notifications = notifications;
    this.feed = feed;
  }

  public PostTarget postTarget(String recipientId, String notificationId) {
    SocialNotificationEntity notification = notifications.findByIdForRecipient(
        recipientId, notificationId).orElseThrow(() -> new IllegalArgumentException("通知不存在"));
    Long postId = switch (notification.type()) {
      case "social.post.liked" -> numericTarget(notification.targetId());
      case "social.post.commented" -> feed.findPostIdByCommentId(numericTarget(notification.targetId()))
          .orElseThrow(() -> new IllegalArgumentException("关联动态不存在"));
      default -> throw new IllegalArgumentException("该通知未关联动态");
    };
    return new PostTarget(postId);
  }

  private Long numericTarget(String targetId) {
    try {
      return Long.parseLong(targetId);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("通知目标无效");
    }
  }
}
