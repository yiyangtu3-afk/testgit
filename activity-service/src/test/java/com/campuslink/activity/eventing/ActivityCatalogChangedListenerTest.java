package com.campuslink.activity.eventing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.campuslink.activity.cache.ActivityCatalogCache;
import org.junit.jupiter.api.Test;

class ActivityCatalogChangedListenerTest {

  @Test
  void delegatesCommittedActivityChangesToTheCatalogCache() {
    ActivityCatalogCache cache = mock(ActivityCatalogCache.class);

    new ActivityCatalogChangedListener(cache).invalidate(new ActivityCatalogChangedEvent("activity-1"));

    verify(cache).invalidate();
  }
}
