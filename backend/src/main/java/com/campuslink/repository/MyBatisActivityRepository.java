package com.campuslink.repository;

import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityReviewEntity;
import com.campuslink.mapper.ActivityMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisActivityRepository implements ActivityRepository {

  private final ActivityMapper activityMapper;

  public MyBatisActivityRepository(ActivityMapper activityMapper) {
    this.activityMapper = activityMapper;
  }

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
    String activityId = newId();
    activityMapper.insertActivity(
        activityId,
        organizerId,
        title,
        description,
        category,
        location,
        startsAt,
        endsAt,
        capacity);
    return findById(activityId).orElseThrow();
  }

  @Override
  public void addReview(String activityId, String actorId, String decision, String reason) {
    activityMapper.insertReview(newId(), activityId, actorId, decision, reason);
  }

  @Override
  public Optional<ActivityEntity> findById(String activityId) {
    return Optional.ofNullable(activityMapper.findById(activityId));
  }

  @Override
  public List<ActivityEntity> findPublished() {
    return activityMapper.findPublished();
  }

  @Override
  public List<ActivityEntity> findPending() {
    return activityMapper.findPending();
  }

  @Override
  public int updateReview(
      String activityId,
      String status,
      String decision,
      String reason,
      String reviewerId) {
    return activityMapper.updateReview(activityId, status, decision, reason, reviewerId);
  }

  @Override
  public List<ActivityReviewEntity> findReviews(String activityId) {
    return activityMapper.findReviews(activityId);
  }

  private String newId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
