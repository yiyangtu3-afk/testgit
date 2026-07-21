package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.data-locations=classpath:empty.sql"
    })
@Transactional
@Rollback
class ModerationRepositoryIntegrationTest {

  @Autowired ModerationRepository moderation;

  @Test
  void persistsReviewerTimeAndCommentWithoutChangingExistingData() {
    var created = moderation.create("post", 9001L, null, "审核元数据集成测试");
    LocalDateTime reviewedAt = LocalDateTime.of(2026, 7, 20, 10, 30);

    moderation.completeReview(
        created.id(), "rejected", "教务管理员", reviewedAt, "请补充活动来源说明");

    var reviewed = moderation.findById(created.id()).orElseThrow();
    assertThat(reviewed.status()).isEqualTo("rejected");
    assertThat(reviewed.reviewerName()).isEqualTo("教务管理员");
    assertThat(reviewed.reviewedAt()).isEqualTo("2026-07-20 10:30");
    assertThat(reviewed.reviewComment()).isEqualTo("请补充活动来源说明");
  }
}
