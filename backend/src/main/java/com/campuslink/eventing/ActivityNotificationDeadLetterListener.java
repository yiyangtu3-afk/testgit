package com.campuslink.eventing;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/** Keeps dead-letter consumption outside the Kafka bean configuration graph. */
@Component
@Profile("eventing")
public class ActivityNotificationDeadLetterListener {

  private final EventDeadLetterService deadLetters;
  private final EventingProperties properties;

  public ActivityNotificationDeadLetterListener(
      EventDeadLetterService deadLetters,
      EventingProperties properties) {
    this.deadLetters = deadLetters;
    this.properties = properties;
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-dead-letter-topic}",
      groupId = "campuslink-activity-notification-dead-letter-v1")
  void record(ConsumerRecord<String, ActivityRegistrationMessage> record) {
    deadLetters.record(record.value(), record.key(),
        header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC, properties.activityTopic()),
        header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE, "Kafka 消费失败"));
  }

  private String header(ConsumerRecord<?, ?> record, String name, String fallback) {
    var header = record.headers().lastHeader(name);
    return header == null ? fallback : new String(header.value(), StandardCharsets.UTF_8);
  }
}
