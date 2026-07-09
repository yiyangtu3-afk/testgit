package com.campuslink.controller;

import com.campuslink.service.DatabaseHealthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

  private final DatabaseHealthService databaseHealthService;

  public DatabaseController(DatabaseHealthService databaseHealthService) {
    this.databaseHealthService = databaseHealthService;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    return databaseHealthService.health();
  }
}
