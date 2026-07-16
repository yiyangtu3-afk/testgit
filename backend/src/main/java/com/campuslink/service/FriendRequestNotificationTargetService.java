package com.campuslink.service;

import com.campuslink.dto.SocialNotificationDtos.FriendRequestTarget;
import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.entity.SocialNotificationEntity;
import com.campuslink.repository.FriendRepository;
import com.campuslink.repository.SocialNotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class FriendRequestNotificationTargetService {

  private final SocialNotificationRepository notifications;
  private final FriendRepository friends;

  public FriendRequestNotificationTargetService(
      SocialNotificationRepository notifications, FriendRepository friends) {
    this.notifications = notifications;
    this.friends = friends;
  }

  public FriendRequestTarget pendingRequestTarget(String recipientId, String notificationId) {
    SocialNotificationEntity notification = notifications.findByIdForRecipient(recipientId, notificationId)
        .orElseThrow(() -> new IllegalArgumentException("通知不存在"));
    if (!"social.friend.requested".equals(notification.type())) {
      throw new IllegalArgumentException("该通知未关联好友申请");
    }
    FriendRequestEntity request = friends.findRequestForRecipient(notification.targetId(), recipientId)
        .orElseThrow(() -> new IllegalArgumentException("关联好友申请不存在"));
    if (!notification.actorId().equals(request.fromUserId())) {
      throw new IllegalArgumentException("关联好友申请无效");
    }
    if (!"pending".equals(request.status())) {
      throw new IllegalArgumentException("该好友申请已处理");
    }
    return new FriendRequestTarget(request.id());
  }
}
