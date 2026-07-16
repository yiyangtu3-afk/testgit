package com.campuslink.repository;

import java.time.LocalDateTime;

public interface AdminMetricsRepository {

  int countUsers();

  int countMessagesSince(LocalDateTime startTime);
}
