package com.campuslink.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityRepository;
import com.campuslink.support.InMemoryAuthSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityOperationsAdminControllerTest {

  @Test void administratorReadsActivityMetricsFromIndependentActivityEndpoint() throws Exception {
    UserEntity admin = new UserEntity("u-admin", "教务管理员", "管理员", "1", "online");
    var sessions = new InMemoryAuthSessionRepository();
    UserRepository users = new UserRepository() {
      public List<UserEntity> findAll() { return List.of(admin); }
      public Optional<UserEntity> findById(String id) { return Optional.of(admin); }
      public Optional<UserEntity> findByPhone(String phone) { return Optional.empty(); }
      public void updatePresence(String userId, String presence) { }
    };
    var service = new ActivityRegistrationService(new InMemoryActivityRepository(),
        new InMemoryActivityRegistrationRepository(),
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var authTokens = new AuthTokenService(sessions, users);
    var mockMvc = MockMvcBuilders.standaloneSetup(new ActivityOperationsAdminController(
            service, authTokens))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    mockMvc.perform(get("/api/admin/activity-metrics")
            .header("Authorization", "Bearer " + authTokens.issueToken(admin.id())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registrationCount").value(0))
        .andExpect(jsonPath("$.checkedInCount").value(0));
  }
}
