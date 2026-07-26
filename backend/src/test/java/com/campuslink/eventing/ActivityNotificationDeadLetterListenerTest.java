package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

class ActivityNotificationDeadLetterListenerTest {

  @Test
  void persistsDeadLetterMetadataWithoutDependingOnKafkaConfiguration() {
    var events = new EventDeadLetterServiceTest.RecordingDeadLetterRepository();
    var deadLetters = new EventDeadLetterService(events, new ObjectMapper().findAndRegisterModules(),
        (topic, key, event) -> { });
    var listener = new ActivityNotificationDeadLetterListener(deadLetters,
        new EventingProperties("campuslink.activity.events.v1", "campuslink.activity.events.v1.DLT",
            1, 3, 100, 3));
    var record = new ConsumerRecord<>("campuslink.activity.events.v1.DLT", 0, 0L, "activity-1",
        new ActivityRegistrationMessage("event-1", "activity.registration.registered.v1", "registration-1",
            "activity-1", "student-1", "student-1", null, "registered",
            LocalDateTime.of(2026, 7, 25, 12, 0), "校园编程赛", null));
    record.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC,
        "campuslink.activity.events.v1".getBytes(StandardCharsets.UTF_8));
    record.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE,
        "数据库不可用".getBytes(StandardCharsets.UTF_8));

    listener.record(record);

    assertThat(events.findDeadLetters(10)).singleElement().satisfies(deadLetter -> {
      assertThat(deadLetter.originalTopic()).isEqualTo("campuslink.activity.events.v1");
      assertThat(deadLetter.failureMessage()).isEqualTo("数据库不可用");
    });
  }
}
