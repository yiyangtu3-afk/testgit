package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.repository.EventProcessingReceiptRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivityEventReceiptServiceTest {

  @Test void recordsOnlyTheFirstLogicalDeliveryOfAnEvent() {
    var receipts = new InMemoryReceiptRepository();
    var service = new ActivityEventReceiptService(receipts);
    var event = new ActivityRegistrationMessage("event-1", "activity.registration.registered.v1",
        "registration-1", "activity-1", "student-1", "student-1", null, "registered",
        LocalDateTime.of(2026, 7, 25, 12, 0), "校园编程赛", 0);

    assertThat(service.recordIfFirst(event)).isTrue();
    assertThat(service.recordIfFirst(event)).isFalse();
    assertThat(receipts.consumerName).isEqualTo(ActivityEventReceiptService.CONSUMER_NAME);
  }

  private static final class InMemoryReceiptRepository implements EventProcessingReceiptRepository {
    private boolean recorded;
    private String consumerName;

    @Override public boolean recordIfFirst(String value, String eventId) {
      consumerName = value;
      if (recorded) {
        return false;
      }
      recorded = true;
      return true;
    }
  }
}
