package com.campuslink.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.ActivityService;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryActivityRegistrationRepository;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import com.campuslink.support.InMemoryActivityRepository;
import com.campuslink.support.InMemoryAuthSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityRegistrationControllerTest {
  private MockMvc mockMvc;
  private String activityId;
  private String studentAuthorization;
  private String teacherAuthorization;

  @BeforeEach void setUp() {
    UserEntity student = new UserEntity("u-student", "林一", "学生", "1", "online");
    UserEntity teacher = new UserEntity("u-teacher", "陈老师", "教师", "2", "online");
    UserEntity admin = new UserEntity("u-admin", "管理员", "管理员", "3", "online");
    var activityRepository = new InMemoryActivityRepository();
    var activityService = new ActivityService(activityRepository,
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var pending = activityService.create(teacher, new CreateActivityRequest("报名测试", "测试", "科技",
        "A201", LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 10, 0), 1));
    activityId = activityService.review(admin, pending.id(), new ReviewActivityRequest("approve", null)).id();
    List<UserEntity> users = List.of(student, teacher, admin);
    var sessions = new InMemoryAuthSessionRepository();
    UserRepository userRepository = new UserRepository() {
      public List<UserEntity> findAll() { return users; }
      public Optional<UserEntity> findById(String id) { return users.stream().filter(u -> u.id().equals(id)).findFirst(); }
      public Optional<UserEntity> findByPhone(String phone) { return Optional.empty(); }
      public void updatePresence(String userId, String presence) { }
    };
    var service = new ActivityRegistrationService(activityRepository,
        new InMemoryActivityRegistrationRepository(),
        new ActivityNotificationService(new InMemoryActivityNotificationRepository()));
    var authTokens = new AuthTokenService(sessions, userRepository);
    studentAuthorization = "Bearer " + authTokens.issueToken(student.id());
    teacherAuthorization = "Bearer " + authTokens.issueToken(teacher.id());
    mockMvc = MockMvcBuilders.standaloneSetup(new ActivityRegistrationController(service,
        authTokens)).setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test void authenticatedStudentRegistersWithoutAttendeeId() throws Exception {
    mockMvc.perform(post("/api/activities/{activityId}/registrations", activityId)
        .header("Authorization", studentAuthorization))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("registered"));
  }

  @Test void teacherCannotRegisterAsAttendee() throws Exception {
    mockMvc.perform(post("/api/activities/{activityId}/registrations", activityId)
        .header("Authorization", teacherAuthorization))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("只有学生可以报名活动"));
  }

  @Test void organizerReadsRosterWithoutClientOrganizerId() throws Exception {
    mockMvc.perform(post("/api/activities/{activityId}/registrations", activityId)
        .header("Authorization", studentAuthorization))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/activities/{activityId}/registrations/roster", activityId)
        .header("Authorization", teacherAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityId").value(activityId))
        .andExpect(jsonPath("$.registeredCount").value(1))
        .andExpect(jsonPath("$.entries[0].attendeeId").value("u-student"));
  }

  @Test void organizerChecksInRegistrationWithoutClientActorId() throws Exception {
    mockMvc.perform(post("/api/activities/{activityId}/registrations", activityId)
        .header("Authorization", studentAuthorization))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/activities/{activityId}/registrations/{registrationId}/check-in",
            activityId, "registration-1")
        .header("Authorization", teacherAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("checked_in"))
        .andExpect(jsonPath("$.checkedInAt").isNotEmpty());
  }
}
