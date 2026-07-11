package com.campuslink.service;

import com.campuslink.dto.ActivityDtos.ActivityView;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ActivityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

  private final ActivityRepository activityRepository;

  public ActivityService(ActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Transactional
  public ActivityView create(UserEntity organizer, CreateActivityRequest request) {
    requireOrganizer(organizer);
    if (!request.endsAt().isAfter(request.startsAt())) {
      throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
    }
    ActivityEntity activity = activityRepository.create(
        organizer.id(),
        request.title().trim(),
        request.description().trim(),
        request.category().trim(),
        request.location().trim(),
        request.startsAt(),
        request.endsAt(),
        request.capacity());
    activityRepository.addReview(activity.id(), organizer.id(), "submitted", null);
    return toView(activity);
  }

  public List<ActivityView> published() {
    return activityRepository.findPublished().stream().map(this::toView).toList();
  }

  public List<ActivityView> pending(UserEntity reviewer) {
    requireAdministrator(reviewer);
    return activityRepository.findPending().stream().map(this::toView).toList();
  }

  @Transactional
  public ActivityView review(
      UserEntity reviewer,
      String activityId,
      ReviewActivityRequest request) {
    requireAdministrator(reviewer);
    ActivityEntity activity = activityRepository.findById(activityId)
        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
    if (!"pending".equals(activity.status()) || !"pending".equals(activity.reviewDecision())) {
      throw new IllegalArgumentException("活动已完成审核");
    }

    String decision = request.decision() == null ? "" : request.decision().trim();
    String status;
    String storedDecision;
    String reason = null;
    if ("approve".equals(decision)) {
      status = "published";
      storedDecision = "approved";
    } else if ("reject".equals(decision)) {
      status = "draft";
      storedDecision = "rejected";
      reason = requireRejectionReason(request.reason());
    } else {
      throw new IllegalArgumentException("审核动作不支持");
    }

    int updated = activityRepository.updateReview(
        activityId,
        status,
        storedDecision,
        reason,
        reviewer.id());
    if (updated != 1) {
      throw new IllegalArgumentException("活动已完成审核");
    }
    activityRepository.addReview(activityId, reviewer.id(), storedDecision, reason);
    return activityRepository.findById(activityId)
        .map(this::toView)
        .orElseThrow(() -> new IllegalStateException("活动审核结果读取失败"));
  }

  private void requireOrganizer(UserEntity user) {
    if (!user.role().contains("教师") && !user.role().contains("社团负责人")) {
      throw new ForbiddenException("只有教师或社团负责人可以创建活动");
    }
  }

  private void requireAdministrator(UserEntity user) {
    if (!user.role().contains("管理员")) {
      throw new ForbiddenException("需要管理员账号审核活动");
    }
  }

  private String requireRejectionReason(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("拒绝活动时必须填写原因");
    }
    return reason.trim();
  }

  private ActivityView toView(ActivityEntity activity) {
    return new ActivityView(
        activity.id(),
        activity.title(),
        activity.description(),
        activity.category(),
        activity.location(),
        activity.startsAt(),
        activity.endsAt(),
        activity.capacity(),
        activity.organizerId(),
        activity.organizerName(),
        activity.status(),
        activity.reviewDecision(),
        activity.reviewReason(),
        activity.reviewerId(),
        activity.reviewerName(),
        activity.reviewedAt(),
        activity.createdAt());
  }
}
