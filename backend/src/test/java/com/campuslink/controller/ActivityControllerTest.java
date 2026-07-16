package com.campuslink.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ActivityService;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryActivityRepository;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryAuthSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityControllerTest {

  private MockMvc mockMvc;
  private String teacherAuthorization;

  @BeforeEach
  void setUp() {
    UserEntity teacher = new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online");
    var authSessions = new InMemoryAuthSessionRepository();
    UserRepository users = new UserRepository() {
      @Override
      public List<UserEntity> findAll() {
        return List.of(teacher);
      }

      @Override
      public Optional<UserEntity> findById(String userId) {
        return "u-2001".equals(userId) ? Optional.of(teacher) : Optional.empty();
      }

      @Override
      public Optional<UserEntity> findByPhone(String phone) {
        return Optional.empty();
      }

      @Override
      public void updatePresence(String userId, String presence) {
      }
    };
    ActivityService activityService = new ActivityService(new InMemoryActivityRepository(),
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    AuthTokenService authTokenService = new AuthTokenService(authSessions, users);
    teacherAuthorization = "Bearer " + authTokenService.issueToken(teacher.id());
    mockMvc = MockMvcBuilders.standaloneSetup(
        new ActivityController(activityService, authTokenService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void teacherSubmissionIgnoresClientOrganizerId() throws Exception {
    mockMvc.perform(post("/api/activities")
            .header("Authorization", teacherAuthorization)
            .contentType("application/json")
            .content("""
                {
                  "title": "校园编程工作坊",
                  "description": "完成一个可运行原型。",
                  "category": "科技",
                  "location": "A201",
                  "organizerId": "u-1001",
                  "startsAt": "2026-08-01T09:00:00",
                  "endsAt": "2026-08-01T12:00:00",
                  "capacity": 40
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizerId").value("u-2001"))
        .andExpect(jsonPath("$.status").value("pending"));
  }

  @Test
  void invalidCapacityReturnsConsistentValidationMessage() throws Exception {
    mockMvc.perform(post("/api/activities")
            .header("Authorization", teacherAuthorization)
            .contentType("application/json")
            .content("""
                {
                  "title": "校园编程工作坊",
                  "description": "完成一个可运行原型。",
                  "category": "科技",
                  "location": "A201",
                  "startsAt": "2026-08-01T09:00:00",
                  "endsAt": "2026-08-01T12:00:00",
                  "capacity": 0
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("活动容量至少为1"));
  }

  @Test
  void teacherReadsOwnPersistedActivities() throws Exception {
    mockMvc.perform(post("/api/activities")
            .header("Authorization", teacherAuthorization)
            .contentType("application/json")
            .content("""
                {"title":"名单工作坊","description":"测试","category":"科技","location":"A201",
                 "startsAt":"2026-08-01T09:00:00","endsAt":"2026-08-01T10:00:00","capacity":10}
                """))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/activities/managed")
            .header("Authorization", teacherAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("名单工作坊"))
        .andExpect(jsonPath("$[0].organizerId").value("u-2001"));
  }
}
