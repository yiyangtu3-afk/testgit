package com.campuslink.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ActivityService;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryActivityRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityAdminControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    UserEntity teacher = new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online");
    UserEntity admin = new UserEntity("u-2003", "教务管理员", "管理员", "13800000004", "online");
    UserEntity student = new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online");
    Map<String, UserEntity> usersById = Map.of(
        teacher.id(), teacher, admin.id(), admin, student.id(), student);
    Map<String, String> tokens = Map.of(
        "teacher-token", teacher.id(), "admin-token", admin.id(), "student-token", student.id());
    AuthSessionRepository authSessions = new AuthSessionRepository() {
      @Override
      public void save(String token, String userId) {
      }

      @Override
      public Optional<String> findUserIdByToken(String token) {
        return Optional.ofNullable(tokens.get(token));
      }
    };
    UserRepository users = new UserRepository() {
      @Override
      public List<UserEntity> findAll() {
        return List.copyOf(usersById.values());
      }

      @Override
      public Optional<UserEntity> findById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
      }

      @Override
      public Optional<UserEntity> findByPhone(String phone) {
        return Optional.empty();
      }

      @Override
      public void updatePresence(String userId, String presence) {
      }
    };
    ActivityService activityService = new ActivityService(new InMemoryActivityRepository());
    AuthTokenService authTokenService = new AuthTokenService(authSessions, users);
    mockMvc = MockMvcBuilders.standaloneSetup(
        new ActivityController(activityService, authTokenService),
        new ActivityAdminController(activityService, authTokenService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void administratorApprovesSubmittedActivity() throws Exception {
    String activityId = submitActivity();

    mockMvc.perform(get("/api/admin/activities/pending")
            .header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(activityId));

    mockMvc.perform(post("/api/admin/activities/{activityId}/reviews", activityId)
            .header("Authorization", "Bearer admin-token")
            .contentType("application/json")
            .content("{\"decision\":\"approve\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("published"))
        .andExpect(jsonPath("$.reviewDecision").value("approved"));

    mockMvc.perform(get("/api/activities")
            .header("Authorization", "Bearer teacher-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(activityId));
  }

  @Test
  void studentCannotReviewActivity() throws Exception {
    String activityId = submitActivity();

    mockMvc.perform(post("/api/admin/activities/{activityId}/reviews", activityId)
            .header("Authorization", "Bearer student-token")
            .contentType("application/json")
            .content("{\"decision\":\"approve\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("需要管理员账号审核活动"));
  }

  @Test
  void rejectionWithoutReasonReturnsBadRequest() throws Exception {
    String activityId = submitActivity();

    mockMvc.perform(post("/api/admin/activities/{activityId}/reviews", activityId)
            .header("Authorization", "Bearer admin-token")
            .contentType("application/json")
            .content("{\"decision\":\"reject\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("拒绝活动时必须填写原因"));
  }

  private String submitActivity() throws Exception {
    MvcResult creation = mockMvc.perform(post("/api/activities")
            .header("Authorization", "Bearer teacher-token")
            .contentType("application/json")
            .content("""
                {
                  "title": "校园编程工作坊",
                  "description": "完成一个可运行原型。",
                  "category": "科技",
                  "location": "A201",
                  "startsAt": "2026-08-01T09:00:00",
                  "endsAt": "2026-08-01T12:00:00",
                  "capacity": 40
                }
                """))
        .andExpect(status().isCreated())
        .andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        creation.getResponse().getContentAsString(), "$.id");
  }
}
