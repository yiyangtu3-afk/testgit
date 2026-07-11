package com.campuslink.repository;

import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityReviewEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository {

  ActivityEntity create(
      String organizerId,
      String title,
      String description,
      String category,
      String location,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      int capacity);

  void addReview(String activityId, String actorId, String decision, String reason);

  Optional<ActivityEntity> findById(String activityId);

  List<ActivityEntity> findPublished();

  List<ActivityEntity> findPending();

  int updateReview(
      String activityId,
      String status,
      String decision,
      String reason,
      String reviewerId);

  List<ActivityReviewEntity> findReviews(String activityId);
}
