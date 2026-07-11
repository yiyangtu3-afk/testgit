package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.ActivityReviewEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.support.InMemoryActivityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {

  private final InMemoryActivityRepository repository = new InMemoryActivityRepository();
  private final ActivityService service = new ActivityService(repository);

  @Test
  void teacherSubmitsPendingActivityAsAuthenticatedOrganizer() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");

    var activity = service.create(teacher, validRequest());

    assertThat(activity.organizerId()).isEqualTo("u-teacher");
    assertThat(activity.organizerName()).isEqualTo("陈老师");
    assertThat(activity.status()).isEqualTo("pending");
    assertThat(activity.reviewDecision()).isEqualTo("pending");
  }

  @Test
  void studentCannotCreateActivity() {
    UserEntity student = new UserEntity("u-student", "林一", "学生账号", "13800000001", "online");

    assertThatThrownBy(() -> service.create(student, validRequest()))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("只有教师或社团负责人可以创建活动");
    assertThat(repository.activities()).isEmpty();
  }

  @Test
  void administratorApprovesPendingActivity() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "13800000004", "online");
    var pending = service.create(teacher, validRequest());

    var published = service.review(admin, pending.id(), new ReviewActivityRequest("approve", null));

    assertThat(published.status()).isEqualTo("published");
    assertThat(published.reviewDecision()).isEqualTo("approved");
    assertThat(published.reviewerId()).isEqualTo("u-admin");
  }

  @Test
  void administratorRejectsActivityBackToDraftAndPreservesReason() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "13800000004", "online");
    var pending = service.create(teacher, validRequest());

    var rejected = service.review(
        admin,
        pending.id(),
        new ReviewActivityRequest("reject", "场地审批材料不完整"));

    assertThat(rejected.status()).isEqualTo("draft");
    assertThat(rejected.reviewDecision()).isEqualTo("rejected");
    assertThat(rejected.reviewReason()).isEqualTo("场地审批材料不完整");
    assertThat(repository.findReviews(pending.id()))
        .extracting(ActivityReviewEntity::decision)
        .containsExactly("submitted", "rejected");
  }

  @Test
  void clubLeaderCanCreateActivity() {
    UserEntity leader = new UserEntity(
        "u-leader", "王社长", "社团负责人", "13800000005", "online");

    var activity = service.create(leader, validRequest());

    assertThat(activity.organizerId()).isEqualTo("u-leader");
    assertThat(activity.status()).isEqualTo("pending");
  }

  @Test
  void administratorCannotCreateActivity() {
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "13800000004", "online");

    assertThatThrownBy(() -> service.create(admin, validRequest()))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("只有教师或社团负责人可以创建活动");
  }

  @Test
  void teacherCannotReviewActivity() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    var pending = service.create(teacher, validRequest());

    assertThatThrownBy(() -> service.review(
        teacher, pending.id(), new ReviewActivityRequest("approve", null)))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("需要管理员账号审核活动");
  }

  @Test
  void rejectionRequiresReasonBeforeStateChanges() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "13800000004", "online");
    var pending = service.create(teacher, validRequest());

    assertThatThrownBy(() -> service.review(
        admin, pending.id(), new ReviewActivityRequest("reject", "  ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("拒绝活动时必须填写原因");
    assertThat(repository.findById(pending.id()).orElseThrow().reviewDecision())
        .isEqualTo("pending");
  }

  @Test
  void reviewedActivityCannotBeReviewedAgain() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "13800000004", "online");
    var pending = service.create(teacher, validRequest());
    service.review(admin, pending.id(), new ReviewActivityRequest("approve", null));

    assertThatThrownBy(() -> service.review(
        admin, pending.id(), new ReviewActivityRequest("reject", "重复审核")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("活动已完成审核");
    assertThat(repository.findReviews(pending.id())).hasSize(2);
  }

  @Test
  void endTimeMustBeAfterStartTime() {
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "13800000002", "online");
    CreateActivityRequest invalid = new CreateActivityRequest(
        "校园编程工作坊",
        "完成一个可运行的校园服务原型。",
        "科技",
        "工程训练中心 A201",
        LocalDateTime.of(2026, 8, 1, 12, 0),
        LocalDateTime.of(2026, 8, 1, 9, 0),
        40);

    assertThatThrownBy(() -> service.create(teacher, invalid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("活动结束时间必须晚于开始时间");
    assertThat(repository.activities()).isEmpty();
  }

  private CreateActivityRequest validRequest() {
    return new CreateActivityRequest(
        "校园编程工作坊",
        "完成一个可运行的校园服务原型。",
        "科技",
        "工程训练中心 A201",
        LocalDateTime.of(2026, 8, 1, 9, 0),
        LocalDateTime.of(2026, 8, 1, 12, 0),
        40);
  }

}
