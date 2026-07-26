package com.campuslink.repository;

import com.campuslink.entity.OutboxEventEntity;
import java.util.List;

public interface OutboxEventRepository {

  void create(String aggregateType, String aggregateId, String eventType, String payload);

  List<OutboxEventEntity> findReady(int limit);

  List<OutboxEventEntity> findDeadLetters(int limit);

  int countByStatus(String status);

  void markPublished(String eventId);

  void markRetryOrDeadLetter(
      String eventId, String message, int retryDelaySeconds, int maxAttempts);

  boolean requeueDeadLetter(String eventId);

  OutboxEventEntity findById(String eventId);
}
