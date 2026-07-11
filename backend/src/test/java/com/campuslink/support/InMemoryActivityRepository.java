package com.campuslink.support;

import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityReviewEntity;
import com.campuslink.repository.ActivityRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InMemoryActivityRepository implements ActivityRepository {

  private final List<ActivityEntity> activities = new ArrayList<>();
  private final List<ActivityReviewEntity> reviews = new ArrayList<>();

  @Override
  public ActivityEntity create(
      String organizerId,
      String title,
      String description,
      String category,
      String location,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      int capacity) {
    ActivityEntity activity = new ActivityEntity(
        "activity-" + (activities.size() + 1),
        title,
        description,
        category,
        location,
        startsAt,
        endsAt,
        capacity,
        organizerId,
        organizerId.contains("leader") ? "社团负责人" : "陈老师",
        "pending",
        "pending",
        null,
        null,
        null,
        null,
        LocalDateTime.of(2026, 7, 10, 9, 0));
    activities.add(activity);
    return activity;
  }

  @Override
  public void addReview(String activityId, String actorId, String decision, String reason) {
    reviews.add(new ActivityReviewEntity(
        "review-" + (reviews.size() + 1),
        activityId,
        actorId,
        decision,
        reason,
        LocalDateTime.of(2026, 7, 10, 9, 0)));
  }

  @Override
  public Optional<ActivityEntity> findById(String activityId) {
    return activities.stream().filter(activity -> activity.id().equals(activityId)).findFirst();
  }

  @Override
  public List<ActivityEntity> findPublished() {
    return activities.stream().filter(activity -> "published".equals(activity.status())).toList();
  }

  @Override
  public List<ActivityEntity> findPending() {
    return activities.stream().filter(activity -> "pending".equals(activity.status())).toList();
  }

  @Override
  public int updateReview(
      String activityId,
      String status,
      String decision,
      String reason,
      String reviewerId) {
    Optional<ActivityEntity> current = findById(activityId)
        .filter(activity -> "pending".equals(activity.status()))
        .filter(activity -> "pending".equals(activity.reviewDecision()));
    if (current.isEmpty()) {
      return 0;
    }
    ActivityEntity activity = current.get();
    ActivityEntity updated = new ActivityEntity(
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
        status,
        decision,
        reason,
        reviewerId,
        "教务管理员",
        LocalDateTime.of(2026, 7, 10, 10, 0),
        activity.createdAt());
    activities.set(activities.indexOf(activity), updated);
    return 1;
  }

  @Override
  public List<ActivityReviewEntity> findReviews(String activityId) {
    return reviews.stream().filter(review -> review.activityId().equals(activityId)).toList();
  }

  public List<ActivityEntity> activities() {
    return List.copyOf(activities);
  }
}
