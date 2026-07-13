package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.service.FeedService;
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
class FeedLikeRepositoryIntegrationTest {

  @Autowired FeedService feed;
  @Autowired SocialNotificationService notifications;

  @Test
  void userScopedLikeAndAuthorNotificationChangeTogetherInsideRollbackTransaction() {
    var post = feed.publish("u-1001", "点赞事务回滚测试动态", "全校可见");

    var liked = feed.likePost(post.id(), "u-2001");

    assertThat(liked.likes()).isEqualTo(1);
    assertThat(liked.likedByCurrentUser()).isTrue();
    assertThat(notifications.summary("u-1001").items().stream()
        .filter(notification -> notification.targetId().equals(String.valueOf(post.id()))))
        .singleElement()
        .satisfies(notification -> {
          assertThat(notification.type()).isEqualTo("social.post.liked");
          assertThat(notification.body()).contains("陈老师");
          assertThat(notification.read()).isFalse();
        });

    var unliked = feed.likePost(post.id(), "u-2001");

    assertThat(unliked.likes()).isZero();
    assertThat(unliked.likedByCurrentUser()).isFalse();
    assertThat(notifications.summary("u-1001").items().stream()
        .filter(notification -> notification.targetId().equals(String.valueOf(post.id()))))
        .hasSize(1);
  }
}
