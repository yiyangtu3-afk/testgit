package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.dto.ActivityDtos.ActivityView;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.ActivityReviewEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.ActivityService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ActivityRepositoryIntegrationTest {

  @Autowired
  private ActivityService activityService;

  @Autowired
  private ActivityRepository activityRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void createsAndApprovesActivityWithRollbackSafeReviewHistory() {
    UserEntity teacher = userRepository.findById("u-2001").orElseThrow();
    UserEntity admin = userRepository.findById("u-2003").orElseThrow();
    CreateActivityRequest request = new CreateActivityRequest(
        "事务测试活动",
        "验证活动与审核历史在同一事务中写入。",
        "测试",
        "工程训练中心 T101",
        LocalDateTime.of(2026, 9, 1, 9, 0),
        LocalDateTime.of(2026, 9, 1, 11, 0),
        12);

    var pending = activityService.create(teacher, request);

    assertThat(pending.organizerId()).isEqualTo("u-2001");
    assertThat(pending.status()).isEqualTo("pending");
    assertThat(activityRepository.findReviews(pending.id()))
        .extracting(ActivityReviewEntity::decision)
        .containsExactly("submitted");

    var published = activityService.review(
        admin, pending.id(), new ReviewActivityRequest("approve", null));

    assertThat(published.status()).isEqualTo("published");
    assertThat(published.reviewerId()).isEqualTo("u-2003");
    assertThat(activityRepository.findReviews(pending.id()))
        .extracting(ActivityReviewEntity::decision)
        .containsExactly("submitted", "approved");
    assertThat(activityService.published())
        .extracting(activity -> activity.id())
        .contains(pending.id());
  }

  @Test
  void rejectionPersistsReasonInCurrentStateAndReviewHistoryUntilRollback() {
    UserEntity teacher = userRepository.findById("u-2001").orElseThrow();
    UserEntity admin = userRepository.findById("u-2003").orElseThrow();
    CreateActivityRequest request = new CreateActivityRequest(
        "事务拒绝测试活动",
        "验证拒绝原因写入当前状态与审核历史。",
        "测试",
        "工程训练中心 T102",
        LocalDateTime.of(2026, 9, 2, 9, 0),
        LocalDateTime.of(2026, 9, 2, 11, 0),
        12);
    var pending = activityService.create(teacher, request);

    var rejected = activityService.review(
        admin,
        pending.id(),
        new ReviewActivityRequest("reject", "缺少场地审批材料"));

    assertThat(rejected.status()).isEqualTo("draft");
    assertThat(rejected.reviewDecision()).isEqualTo("rejected");
    assertThat(rejected.reviewReason()).isEqualTo("缺少场地审批材料");
    assertThat(activityRepository.findReviews(pending.id()).getLast().reason())
        .isEqualTo("缺少场地审批材料");
  }

  @Test
  void filtersPublishedActivitiesByCategoryAndInclusiveDateRangeUntilRollback() {
    UserEntity teacher = userRepository.findById("u-2001").orElseThrow();
    UserEntity admin = userRepository.findById("u-2003").orElseThrow();
    var technology = createAndApprove(
        teacher, admin, "MyBatis 科技筛选测试", "科技", LocalDateTime.of(2026, 10, 1, 9, 0));
    var publicService = createAndApprove(
        teacher, admin, "MyBatis 公益筛选测试", "公益", LocalDateTime.of(2026, 10, 2, 9, 0));

    var filtered = activityService.published(
        "公益", LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2));

    assertThat(filtered).extracting(activity -> activity.id())
        .contains(publicService.id())
        .doesNotContain(technology.id());
  }

  private ActivityView createAndApprove(
      UserEntity teacher,
      UserEntity admin,
      String title,
      String category,
      LocalDateTime startsAt) {
    var pending = activityService.create(teacher, new CreateActivityRequest(
        title,
        "验证公开活动 MyBatis 筛选。",
        category,
        "工程训练中心 T103",
        startsAt,
        startsAt.plusHours(2),
        12));
    return activityService.review(
        admin, pending.id(), new ReviewActivityRequest("approve", null));
  }
}
