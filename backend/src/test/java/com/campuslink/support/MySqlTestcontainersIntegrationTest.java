package com.campuslink.support;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class MySqlTestcontainersIntegrationTest {

  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("campuslink_test")
      .withUsername("campuslink")
      .withPassword("campuslink123");

  static {
    MYSQL.start();
  }

  @DynamicPropertySource
  static void configureMySql(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    registry.add("spring.sql.init.mode", () -> "always");
  }

  @AfterAll
  static void stopMySql() {
    MYSQL.stop();
  }
}
