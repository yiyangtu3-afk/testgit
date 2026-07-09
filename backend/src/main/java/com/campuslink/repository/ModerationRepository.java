package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import java.util.List;
import java.util.Optional;

public interface ModerationRepository {

  List<ModerationItemEntity> findAll();

  List<ModerationItemEntity> findPending();

  long countPending();

  ModerationItemEntity create(String type, Long targetId, Long postId, String reason);

  Optional<ModerationItemEntity> findById(String itemId);

  void updateStatus(String itemId, String status);

  int deleteByIds(List<String> itemIds);
}
