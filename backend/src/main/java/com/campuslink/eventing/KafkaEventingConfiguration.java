package com.campuslink.eventing;

import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableKafka
@Profile("eventing")
@EnableConfigurationProperties(EventingProperties.class)
public class KafkaEventingConfiguration {

  private final ActivityEventReceiptService receipts;

  public KafkaEventingConfiguration(ActivityEventReceiptService receipts) {
    this.receipts = receipts;
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

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityEventReceiptService.CONSUMER_NAME)
  void recordActivityEvent(ActivityRegistrationMessage event) {
    receipts.recordIfFirst(event);
  }
}
