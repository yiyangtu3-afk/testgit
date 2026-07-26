package com.campuslink.activity.service;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.campuslink.activity.api.ActivityDtos.CreateActivityRequest;
import com.campuslink.activity.api.ActivityDtos.ReviewActivityRequest;
import com.campuslink.activity.domain.ActivityRecord;
import com.campuslink.activity.domain.UserDirectoryEntry;
import com.campuslink.activity.eventing.ActivityReviewEvent;
import com.campuslink.activity.eventing.ActivityReviewEventPublisher;
import com.campuslink.activity.mapper.ActivityMapper;
import com.campuslink.activity.mapper.UserDirectoryMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityApplicationService {
  private final ActivityMapper activities;
  private final UserDirectoryMapper users;
  private final ActivityReviewEventPublisher reviewEvents;
  public ActivityApplicationService(ActivityMapper activities, UserDirectoryMapper users, ActivityReviewEventPublisher reviewEvents) { this.activities = activities; this.users = users; this.reviewEvents = reviewEvents; }

  public List<ActivityView> published(String category, LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException("活动筛选结束日期不能早于开始日期");
    return activities.published(blankToNull(category), from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay()).stream().map(this::view).toList();
  }
  @Transactional
  public ActivityView create(UserDirectoryEntry organizer, CreateActivityRequest request) {
    organizer(organizer);
    if (!request.endsAt().isAfter(request.startsAt())) throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
    String id = id();
    activities.insert(id, organizer.id(), trim(request.title()), trim(request.description()), trim(request.category()), trim(request.location()), request.startsAt(), request.endsAt(), request.capacity());
    activities.insertReview(id(), id, organizer.id(), "submitted", null);
    return view(required(id));
  }
  public List<ActivityView> managed(UserDirectoryEntry organizer) { organizer(organizer); return activities.managed(organizer.id()).stream().map(this::view).toList(); }
  public List<ActivityView> pending(UserDirectoryEntry reviewer) { administrator(reviewer); return activities.pending().stream().map(this::view).toList(); }
  @Transactional
  public ActivityView review(UserDirectoryEntry reviewer, String id, ReviewActivityRequest request) {
    administrator(reviewer);
    ActivityRecord original = required(id);
    if (!"pending".equals(original.status()) || !"pending".equals(original.reviewDecision())) throw new IllegalArgumentException("活动已完成审核");
    String decision = trim(request.decision()); String status; String stored; String reason = null;
    if ("approve".equals(decision)) { status = "published"; stored = "approved"; }
    else if ("reject".equals(decision)) { status = "draft"; stored = "rejected"; reason = rejectionReason(request.reason()); }
    else throw new IllegalArgumentException("审核动作不支持");
    if (activities.review(id, status, stored, reason, reviewer.id()) != 1) throw new IllegalArgumentException("活动已完成审核");
    activities.insertReview(id(), id, reviewer.id(), stored, reason);
    ActivityRecord reviewed = required(id);
    reviewEvents.publish(new ActivityReviewEvent(reviewed, reviewer.id(), LocalDateTime.now()));
    return view(reviewed);
  }
  private ActivityRecord required(String id) { ActivityRecord result = activities.find(id); if (result == null) throw new IllegalArgumentException("活动不存在"); return result; }
  private ActivityView view(ActivityRecord a) { UserDirectoryEntry organizer = users.find(a.organizerId()); UserDirectoryEntry reviewer = a.reviewerId() == null ? null : users.find(a.reviewerId()); return new ActivityView(a.id(), a.title(), a.description(), a.category(), a.location(), a.startsAt(), a.endsAt(), a.capacity(), a.organizerId(), organizer == null ? null : organizer.name(), a.status(), a.reviewDecision(), a.reviewReason(), a.reviewerId(), reviewer == null ? null : reviewer.name(), a.reviewedAt(), a.createdAt()); }
  private void organizer(UserDirectoryEntry user) { if (!user.role().contains("教师") && !user.role().contains("社团负责人")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有教师或社团负责人可以创建活动"); }
  private void administrator(UserDirectoryEntry user) { if (!user.role().contains("管理员")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员账号审核活动"); }
  private String rejectionReason(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("拒绝活动时必须填写原因"); return trim(value); }
  private String id() { return UUID.randomUUID().toString().replace("-", ""); }
  private String trim(String value) { return value.trim(); }
  private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
