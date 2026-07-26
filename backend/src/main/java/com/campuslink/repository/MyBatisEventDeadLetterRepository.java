package com.campuslink.repository;

import com.campuslink.entity.EventDeadLetterEntity;
import com.campuslink.mapper.EventDeadLetterMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEventDeadLetterRepository implements EventDeadLetterRepository {

  private final EventDeadLetterMapper mapper;

  public MyBatisEventDeadLetterRepository(EventDeadLetterMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void record(
      String consumerName,
      String eventId,
      String eventType,
      String eventKey,
      String originalTopic,
      String payload,
      String failureMessage) {
    mapper.upsert(UUID.randomUUID().toString().replace("-", ""), consumerName, eventId, eventType,
        eventKey, originalTopic, payload, failureMessage);
  }

  @Override
  public List<EventDeadLetterEntity> findDeadLetters(int limit) {
    return mapper.findDeadLetters(limit);
  }

  @Override
  public Optional<EventDeadLetterEntity> findById(String id) {
    return Optional.ofNullable(mapper.findById(id));
  }

  @Override
  public boolean markReplayed(String id) {
    return mapper.markReplayed(id) == 1;
  }

  @Override
  public int countDeadLetters() {
    return mapper.countDeadLetters();
  }
}
