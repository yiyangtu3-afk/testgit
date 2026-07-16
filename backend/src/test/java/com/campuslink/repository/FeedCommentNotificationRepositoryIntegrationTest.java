package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.service.FeedService;
import com.campuslink.service.SocialNotificationService;
import com.campuslink.service.SocialNotificationTargetService;
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
class FeedCommentNotificationRepositoryIntegrationTest {

  @Autowired FeedService feed;
  @Autowired SocialNotificationService notifications;
  @Autowired SocialNotificationTargetService targets;

  @Test
  void commentModerationAuditAndAuthorNotificationRollBackTogether() {
    var post = feed.publish("u-1001", "评论通知事务回滚测试动态", "全校可见");

    var comment = feed.publishComment(post.id(), "u-2001", "请补充活动时间说明。");

    assertThat(comment.moderationStatus()).isEqualTo("pending");
    assertThat(feed.comments(post.id())).isEmpty();
    var notification = notifications.summary("u-1001").items().stream()
        .filter(item -> item.targetId().equals(String.valueOf(comment.id())))
        .findFirst()
        .orElseThrow();
    assertThat(notification).satisfies(item -> {
      assertThat(item.type()).isEqualTo("social.post.commented");
      assertThat(item.title()).isEqualTo("动态收到新评论");
      assertThat(item.body()).contains("陈老师");
      assertThat(item.read()).isFalse();
    });
    assertThat(targets.postTarget("u-1001", notification.id()).postId()).isEqualTo(post.id());
    notifications.markRead("u-1001", notification.id());
    assertThat(notifications.summary("u-1001").items()).anySatisfy(item -> {
      assertThat(item.id()).isEqualTo(notification.id());
      assertThat(item.read()).isTrue();
    });
  }
}
