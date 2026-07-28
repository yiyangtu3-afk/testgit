package com.campuslink.activity.eventing;

import com.campuslink.activity.cache.ActivityCatalogCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActivityCatalogChangedListener {
  private final ActivityCatalogCache cache;

  public ActivityCatalogChangedListener(ActivityCatalogCache cache) {
    this.cache = cache;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void invalidate(ActivityCatalogChangedEvent ignored) {
    cache.invalidate();
  }
}
