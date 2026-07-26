package com.campuslink.notification.eventing;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class NotificationKafkaConfiguration {
  @Bean
  NewTopic notificationDeliveryTopic(
      @Value("${campuslink.eventing.activity-notification-delivery-topic}") String topic) {
    return TopicBuilder.name(topic).partitions(3).replicas(1)
        .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE).build();
  }

  @Bean
  DefaultErrorHandler notificationProjectionErrorHandler(
      KafkaTemplate<String, ActivityRegistrationMessage> template,
      @Value("${campuslink.eventing.activity-dead-letter-topic}") String deadLetterTopic,
      @Value("${campuslink.eventing.consumer-retry-delay-ms}") long retryDelay,
      @Value("${campuslink.eventing.consumer-max-attempts}") int maxAttempts) {
    var recoverer = new DeadLetterPublishingRecoverer(template,
        (record, error) -> new TopicPartition(deadLetterTopic, record.partition()));
    return new DefaultErrorHandler(recoverer, new FixedBackOff(retryDelay, Math.max(0, maxAttempts - 1)));
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, ActivityRegistrationMessage>
      notificationKafkaListenerContainerFactory(
          ConsumerFactory<String, ActivityRegistrationMessage> consumerFactory,
          DefaultErrorHandler notificationProjectionErrorHandler) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, ActivityRegistrationMessage>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(notificationProjectionErrorHandler);
    return factory;
  }
}
