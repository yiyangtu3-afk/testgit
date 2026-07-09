package com.campuslink.service;

import com.campuslink.mapper.DatabaseHealthMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

  private final DatabaseHealthMapper databaseHealthMapper;

  public DatabaseHealthService(DatabaseHealthMapper databaseHealthMapper) {
    this.databaseHealthMapper = databaseHealthMapper;
  }

  public Map<String, Object> health() {
    String databaseName = databaseHealthMapper.databaseName();
    int userCount = databaseHealthMapper.countUsers();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", "UP");
    result.put("database", databaseName);
    result.put("demoUsers", userCount);
    return result;
  }
}
