package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import com.campuslink.mapper.ModerationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisModerationRepository implements ModerationRepository {

  private final ModerationMapper moderationMapper;
  private final AtomicLong moderationIds = new AtomicLong(System.currentTimeMillis() * 1000);

  public MyBatisModerationRepository(ModerationMapper moderationMapper) {
    this.moderationMapper = moderationMapper;
  }

  @Override
  public List<ModerationItemEntity> findAll() {
    return moderationMapper.findAll();
  }

  @Override
  public List<ModerationItemEntity> findPending() {
    return moderationMapper.findPending();
  }

  @Override
  public long countPending() {
    return moderationMapper.countPending();
  }

  @Override
  public ModerationItemEntity create(String type, Long targetId, Long postId, String reason) {
    String itemId = "mod-" + moderationIds.incrementAndGet();
    moderationMapper.insert(itemId, type, String.valueOf(targetId), reason);
    return findById(itemId).orElseThrow();
  }

  @Override
  public Optional<ModerationItemEntity> findById(String itemId) {
    return Optional.ofNullable(moderationMapper.findById(itemId));
  }

  @Override
  public void completeReview(
      String itemId,
      String status,
      String reviewerName,
      LocalDateTime reviewedAt,
      String reviewComment) {
    moderationMapper.completeReview(itemId, status, reviewerName, reviewedAt, reviewComment);
  }

  @Override
  public int deleteByIds(List<String> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return 0;
    }
    return moderationMapper.deleteByIds(itemIds);
  }
}
