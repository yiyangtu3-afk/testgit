package com.campuslink.eventing;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Profile("eventing")
@EnableConfigurationProperties(EventingProperties.class)
public class KafkaEventingConfiguration {

  private final ActivityEventReceiptService receipts;
  private final ActivityRegistrationNotificationProjection notificationProjection;
  private final EventDeadLetterService deadLetters;
  private final EventingProperties properties;

  public KafkaEventingConfiguration(
      ActivityEventReceiptService receipts,
      ActivityRegistrationNotificationProjection notificationProjection,
      EventDeadLetterService deadLetters,
      EventingProperties properties) {
    this.receipts = receipts;
    this.notificationProjection = notificationProjection;
    this.deadLetters = deadLetters;
    this.properties = properties;
  }

  @Bean
  NewTopic activityEventsTopic(EventingProperties properties) {
    return TopicBuilder.name(properties.activityTopic())
        .partitions(3)
        .replicas(1)
        .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
        .build();
  }

  @Bean
  NewTopic activityDeadLetterTopic(EventingProperties properties) {
    return TopicBuilder.name(properties.activityDeadLetterTopic())
        .partitions(3)
        .replicas(1)
        .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
        .build();
  }

  @Bean
  OutboxEventTransport kafkaActivityEventTransport(
      KafkaTemplate<String, ActivityRegistrationMessage> kafkaTemplate,
      EventingProperties properties) {
    return event -> {
      try {
        kafkaTemplate.send(properties.activityTopic(), event.activityId(), event)
            .get(10, TimeUnit.SECONDS);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Kafka 事件发布被中断", error);
      } catch (Exception error) {
        throw new IllegalStateException("Kafka 事件发布失败", error);
      }
    };
  }

  @Bean
  EventReplayTransport kafkaEventReplayTransport(
      KafkaTemplate<String, ActivityRegistrationMessage> kafkaTemplate) {
    return (topic, key, event) -> {
      try {
        kafkaTemplate.send(topic, key, event).get(10, TimeUnit.SECONDS);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Kafka 死信事件重放被中断", error);
      } catch (Exception error) {
        throw new IllegalStateException("Kafka 死信事件重放失败", error);
      }
    };
  }

  @Bean
  DefaultErrorHandler activityNotificationErrorHandler(
      KafkaTemplate<String, ActivityRegistrationMessage> kafkaTemplate) {
    var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
        (record, error) -> new TopicPartition(properties.activityDeadLetterTopic(), record.partition()));
    int retries = Math.max(0, properties.consumerMaxAttempts() - 1);
    return new DefaultErrorHandler(recoverer,
        new FixedBackOff(properties.consumerRetryDelayMs(), retries));
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, ActivityRegistrationMessage>
      activityNotificationKafkaListenerContainerFactory(
          ConsumerFactory<String, ActivityRegistrationMessage> consumerFactory,
          DefaultErrorHandler activityNotificationErrorHandler) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, ActivityRegistrationMessage>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(activityNotificationErrorHandler);
    return factory;
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityEventReceiptService.CONSUMER_NAME)
  void recordActivityEvent(ActivityRegistrationMessage event) {
    receipts.recordIfFirst(event);
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityRegistrationNotificationProjection.CONSUMER_NAME,
      containerFactory = "activityNotificationKafkaListenerContainerFactory")
  void projectActivityNotification(ActivityRegistrationMessage event) {
    notificationProjection.project(event);
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-dead-letter-topic}",
      groupId = "campuslink-activity-notification-dead-letter-v1")
  void recordActivityNotificationDeadLetter(ConsumerRecord<String, ActivityRegistrationMessage> record) {
    deadLetters.record(record.value(), record.key(),
        header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC, properties.activityTopic()),
        header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE, "Kafka 消费失败"));
  }

  private String header(ConsumerRecord<?, ?> record, String name, String fallback) {
    var header = record.headers().lastHeader(name);
    return header == null ? fallback : new String(header.value(), StandardCharsets.UTF_8);
  }
}
