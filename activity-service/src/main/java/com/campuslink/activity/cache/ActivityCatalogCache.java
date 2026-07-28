package com.campuslink.activity.cache;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

public interface ActivityCatalogCache {
  List<ActivityView> load(
      String category, LocalDate from, LocalDate to, Supplier<List<ActivityView>> databaseLoader);

  void invalidate();
}
