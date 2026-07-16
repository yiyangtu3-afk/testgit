package com.campuslink.repository;

import com.campuslink.mapper.AdminMetricsMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAdminMetricsRepository implements AdminMetricsRepository {

  private final AdminMetricsMapper mapper;

  public MyBatisAdminMetricsRepository(AdminMetricsMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public int countUsers() {
    return mapper.countUsers();
  }

  @Override
  public int countMessagesSince(LocalDateTime startTime) {
    return mapper.countMessagesSince(startTime);
  }
}
