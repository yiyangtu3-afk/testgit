package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.support.InMemorySocialNotificationRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SocialNotificationServiceTest {

  @Test
  void postCommentNotificationPublishesPersistedNotificationEvent() {
    List<Object> publishedEvents = new ArrayList<>();
    ApplicationEventPublisher events = publishedEvents::add;
    SocialNotificationService notifications = new SocialNotificationService(
        new InMemorySocialNotificationRepository(), events);

    notifications.recordPostCommented(
        "u-1001", "u-2001", "陈老师", 42L, "活动地点已更新到图书馆一楼。 ");

    assertThat(publishedEvents).singleElement()
        .isInstanceOfSatisfying(SocialNotificationCreatedEvent.class, event -> {
          assertThat(event.recipientId()).isEqualTo("u-1001");
          assertThat(event.notification().targetId()).isEqualTo("42");
          assertThat(event.notification().type()).isEqualTo("social.post.commented");
          assertThat(event.notification().body()).contains("陈老师评论了你的动态");
          assertThat(event.notification().read()).isFalse();
        });
  }
}
