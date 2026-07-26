package com.campuslink.eventing;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Hosts Kafka listener methods outside configuration bean creation. */
@Component
@Profile("eventing")
public class ActivityEventKafkaListeners {

  private final ActivityEventReceiptService receipts;
  private final ActivityRegistrationNotificationProjection notificationProjection;

  public ActivityEventKafkaListeners(
      ActivityEventReceiptService receipts,
      ActivityRegistrationNotificationProjection notificationProjection) {
    this.receipts = receipts;
    this.notificationProjection = notificationProjection;
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityEventReceiptService.CONSUMER_NAME)
  void recordReceipt(ActivityRegistrationMessage event) {
    receipts.recordIfFirst(event);
  }

  @KafkaListener(
      topics = "${campuslink.eventing.activity-topic}",
      groupId = ActivityRegistrationNotificationProjection.CONSUMER_NAME,
      containerFactory = "activityNotificationKafkaListenerContainerFactory")
  void projectNotification(ActivityRegistrationMessage event) {
    notificationProjection.project(event);
  }
}
