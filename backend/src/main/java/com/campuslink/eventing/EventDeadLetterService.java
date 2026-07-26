package com.campuslink.eventing;

import com.campuslink.entity.EventDeadLetterEntity;
import com.campuslink.repository.EventDeadLetterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventDeadLetterService {

  public static final String CONSUMER_NAME = "campuslink-activity-notification-v1";

  private final EventDeadLetterRepository events;
  private final ObjectMapper objectMapper;
  private final EventReplayTransport replayTransport;

  public EventDeadLetterService(
      EventDeadLetterRepository events,
      ObjectMapper objectMapper,
      EventReplayTransport replayTransport) {
    this.events = events;
    this.objectMapper = objectMapper;
    this.replayTransport = replayTransport;
  }

  @Transactional
  public void record(
      ActivityRegistrationMessage event,
      String eventKey,
      String originalTopic,
      String failureMessage) {
    events.record(CONSUMER_NAME, event.eventId(), event.eventType(), eventKey, originalTopic,
        json(event), failureMessage);
  }

  public List<EventDeadLetterEntity> deadLetters(int limit) {
    return events.findDeadLetters(limit);
  }

  public int countDeadLetters() {
    return events.countDeadLetters();
  }

  @Transactional
  public void replay(String id) {
    EventDeadLetterEntity event = events.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("消费者死信事件不存在"));
    if (!"dead_letter".equals(event.status())) {
      throw new IllegalArgumentException("消费者死信事件已重放");
    }
    replayTransport.replay(event.originalTopic(), event.eventKey(), read(event.payload()));
    if (!events.markReplayed(id)) {
      throw new IllegalStateException("消费者死信事件重放状态更新失败");
    }
  }

  private String json(ActivityRegistrationMessage event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("无法保存消费者死信事件", error);
    }
  }

  private ActivityRegistrationMessage read(String payload) {
    try {
      return objectMapper.readValue(payload, ActivityRegistrationMessage.class);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("消费者死信事件载荷无效", error);
    }
  }
}
