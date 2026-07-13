package com.campuslink.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActivityNotificationRealtimeListener {

  private final ActivityNotificationRealtimePublisher realtime;

  public ActivityNotificationRealtimeListener(ActivityNotificationRealtimePublisher realtime) {
    this.realtime = realtime;
  }

  @TransactionalEventListener(
      phase = TransactionPhase.AFTER_COMMIT,
      fallbackExecution = true)
  public void afterNotificationCommitted(ActivityNotificationCreatedEvent event) {
    realtime.publishActivityNotification(event.recipientId(), event.notification());
  }
}
