package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.SocialNotificationDtos.NotificationView;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SocialNotificationRealtimeListenerTest {

  @Test
  void forwardsCommittedNotificationOnlyToItsRecipient() {
    CapturingPublisher realtime = new CapturingPublisher();
    SocialNotificationRealtimeListener listener = new SocialNotificationRealtimeListener(realtime);
    NotificationView notification = new NotificationView(
        "social-notification-1",
        "post-1",
        "social.post.liked",
        "动态收到新点赞",
        "周同学赞了你的动态。",
        false,
        LocalDateTime.of(2026, 7, 15, 11, 0));

    listener.afterNotificationCommitted(new SocialNotificationCreatedEvent("u-1001", notification));

    assertThat(realtime.recipientId).isEqualTo("u-1001");
    assertThat(realtime.notification).isEqualTo(notification);
  }

  private static final class CapturingPublisher implements SocialNotificationRealtimePublisher {

    private String recipientId;
    private NotificationView notification;

    @Override
    public void publishSocialNotification(String recipientId, NotificationView notification) {
      this.recipientId = recipientId;
      this.notification = notification;
    }
  }
}
