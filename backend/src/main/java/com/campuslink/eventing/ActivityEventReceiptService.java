package com.campuslink.eventing;

import com.campuslink.repository.EventProcessingReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityEventReceiptService {

  public static final String CONSUMER_NAME = "campuslink-activity-event-receipt-v1";

  private final EventProcessingReceiptRepository receipts;

  public ActivityEventReceiptService(EventProcessingReceiptRepository receipts) {
    this.receipts = receipts;
  }

  @Transactional
  public boolean recordIfFirst(ActivityRegistrationMessage event) {
    return recordIfFirst(CONSUMER_NAME, event.eventId());
  }

  @Transactional
  public boolean recordIfFirst(String consumerName, String eventId) {
    return receipts.recordIfFirst(consumerName, eventId);
  }
}
