package com.campuslink.activity.cache;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "campuslink.redis.activity-catalog.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoopActivityCatalogCache implements ActivityCatalogCache {
  @Override
  public List<ActivityView> load(
      String category, LocalDate from, LocalDate to, Supplier<List<ActivityView>> databaseLoader) {
    return databaseLoader.get();
  }

  @Override
  public void invalidate() {
    // Native development keeps Redis optional, so there is no cache to invalidate.
  }
}
