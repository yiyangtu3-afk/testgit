package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.service.AdminService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
class AdminMetricsRepositoryIntegrationTest {

  @Autowired AdminMetricsRepository metrics;
  @Autowired ChatRepository chat;
  @Autowired UserRepository users;
  @Autowired FeedRepository feed;
  @Autowired ModerationRepository moderation;
  @Autowired AdminService adminService;

  @Test
  void countsCurrentUsersAndMessagesCreatedTodayWithoutPersistingTestData() {
    LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
    int messageCount = metrics.countMessagesSince(startOfToday);

    MessageEntity saved = chat.saveMessage("u-2001", "u-1001", "指标集成测试消息", List.of());

    assertThat(metrics.countUsers()).isEqualTo(users.findAll().size());
    assertThat(metrics.countMessagesSince(startOfToday)).isEqualTo(messageCount + 1);
    assertThat(saved.text()).isEqualTo("指标集成测试消息");
  }

  @Test
  void dashboardMetricsUseCurrentMysqlCounts() {
    Map<String, String> dashboard = adminService.metrics();
    LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

    assertThat(dashboard).containsEntry("注册用户", String.valueOf(metrics.countUsers()));
    assertThat(dashboard).containsEntry(
        "今日消息", String.valueOf(metrics.countMessagesSince(startOfToday)));
    assertThat(dashboard).containsEntry("动态总数", String.valueOf(feed.countPosts()));
    assertThat(dashboard).containsEntry("待审内容", String.valueOf(moderation.countPending()));
  }
}
