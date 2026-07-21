package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ModerationRepository {

  List<ModerationItemEntity> findAll();

  List<ModerationItemEntity> findPending();

  long countPending();

  ModerationItemEntity create(String type, Long targetId, Long postId, String reason);

  Optional<ModerationItemEntity> findById(String itemId);

  void completeReview(
      String itemId,
      String status,
      String reviewerName,
      LocalDateTime reviewedAt,
      String reviewComment);

  int deleteByIds(List<String> itemIds);
}
