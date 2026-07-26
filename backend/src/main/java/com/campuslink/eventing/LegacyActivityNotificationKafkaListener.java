package com.campuslink.eventing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Compatibility projection used until notification-service is enabled for an environment.
 *
 * <p>Compose disables this bean so the extracted service is the only member of the notification
 * consumer group. Keeping the switch on by default preserves existing local eventing behaviour.
 */
@Component
@Profile("eventing")
@ConditionalOnProperty(
    name = "campuslink.eventing.notification-projection.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class LegacyActivityNotificationKafkaListener {

  private final ActivityRegistrationNotificationProjection notificationProjection;

  public LegacyActivityNotificationKafkaListener(
      ActivityRegistrationNotificationProjection notificationProjection) {
    this.notificationProjection = notificationProjection;
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityRegistrationNotificationProjection.CONSUMER_NAME,
      containerFactory = "activityNotificationKafkaListenerContainerFactory")
  void projectNotification(ActivityRegistrationMessage event) {
    notificationProjection.project(event);
  }
}
