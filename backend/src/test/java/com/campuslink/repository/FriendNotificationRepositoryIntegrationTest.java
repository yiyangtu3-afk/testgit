package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.CampusLinkApplication;
import com.campuslink.service.FriendService;
import com.campuslink.service.FriendRequestNotificationTargetService;
import com.campuslink.service.SocialNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.sql.init.mode=never")
@Transactional
@Rollback
class FriendNotificationRepositoryIntegrationTest {

  @Autowired FriendService friends;
  @Autowired SocialNotificationService notifications;
  @Autowired FriendRequestNotificationTargetService friendRequestTargets;

  @Test
  void requestAcceptanceAndNotificationsRollBackTogether() {
    friends.createFriendRequest("u-2004", "u-2002");
    var request = friends.friendRequests("u-2002").stream()
        .filter(item -> item.fromUserId().equals("u-2004"))
        .findFirst()
        .orElseThrow();

    var receivedNotification = notifications.summary("u-2002").items().stream()
        .filter(notification -> notification.type().equals("social.friend.requested"))
        .filter(notification -> notification.targetId().equals(request.id()))
        .findFirst()
        .orElseThrow();
    assertThat(friendRequestTargets.pendingRequestTarget("u-2002", receivedNotification.id()).requestId())
        .isEqualTo(request.id());

    friends.acceptFriendRequest(request.id(), "u-2002");

    assertThatThrownBy(() -> friendRequestTargets.pendingRequestTarget("u-2002", receivedNotification.id()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("该好友申请已处理");

    assertThat(notifications.summary("u-2002").items()).anySatisfy(notification -> {
      assertThat(notification.targetId()).isEqualTo(request.id());
      assertThat(notification.type()).isEqualTo("social.friend.requested");
      assertThat(notification.body()).contains("王社长");
    });
    assertThat(notifications.summary("u-2004").items()).anySatisfy(notification -> {
      assertThat(notification.targetId()).isEqualTo(request.id());
      assertThat(notification.type()).isEqualTo("social.friend.accepted");
      assertThat(notification.body()).contains("周同学");
      assertThat(notification.read()).isFalse();
    });
  }
}
