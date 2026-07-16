package com.campuslink.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SocialNotificationRealtimeListener {

  private final SocialNotificationRealtimePublisher realtime;

  public SocialNotificationRealtimeListener(SocialNotificationRealtimePublisher realtime) {
    this.realtime = realtime;
  }

  @TransactionalEventListener(
      phase = TransactionPhase.AFTER_COMMIT,
      fallbackExecution = true)
  public void afterNotificationCommitted(SocialNotificationCreatedEvent event) {
    realtime.publishSocialNotification(event.recipientId(), event.notification());
  }
}
