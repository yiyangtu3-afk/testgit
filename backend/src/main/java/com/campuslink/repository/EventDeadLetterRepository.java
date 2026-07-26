package com.campuslink.repository;

import com.campuslink.entity.EventDeadLetterEntity;
import java.util.List;
import java.util.Optional;

public interface EventDeadLetterRepository {

  void record(
      String consumerName,
      String eventId,
      String eventType,
      String eventKey,
      String originalTopic,
      String payload,
      String failureMessage);

  List<EventDeadLetterEntity> findDeadLetters(int limit);

  Optional<EventDeadLetterEntity> findById(String id);

  boolean markReplayed(String id);

  int countDeadLetters();
}
