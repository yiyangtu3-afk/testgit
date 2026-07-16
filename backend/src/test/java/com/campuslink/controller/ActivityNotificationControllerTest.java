package com.campuslink.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.ActivityNotificationService;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryActivityNotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActivityNotificationControllerTest {

  private MockMvc mockMvc;
  private InMemoryActivityNotificationRepository notifications;

  @BeforeEach
  void setUp() {
    UserEntity student = new UserEntity("u-student", "林一", "学生", "1", "online");
    UserEntity other = new UserEntity("u-other", "周同学", "学生", "2", "online");
    notifications = new InMemoryActivityNotificationRepository();
    AuthSessionRepository sessions = new AuthSessionRepository() {
      public void save(String token, String userId) { }
      public Optional<String> findUserIdByToken(String token) {
        return "student-token".equals(token) ? Optional.of(student.id()) : Optional.empty();
      }
    };
    UserRepository users = new UserRepository() {
      public List<UserEntity> findAll() { return List.of(student, other); }
      public Optional<UserEntity> findById(String id) {
        return findAll().stream().filter(user -> user.id().equals(id)).findFirst();
      }
      public Optional<UserEntity> findByPhone(String phone) { return Optional.empty(); }
      public void updatePresence(String userId, String presence) { }
    };
    var service = new ActivityNotificationService(notifications);
    mockMvc = MockMvcBuilders.standaloneSetup(new ActivityNotificationController(
            service, new AuthTokenService(sessions, users)))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void authenticatedUserSeesOnlyOwnUnreadActivityNotifications() throws Exception {
    notifications.create("u-student", "activity-1", "activity.review.approved",
        "活动已发布", "校园编程工作坊已通过审核。");
    notifications.create("u-other", "activity-2", "activity.registration.promoted",
        "候补已递补", "你已获得活动名额。");

    mockMvc.perform(get("/api/activity-notifications")
            .header("Authorization", "Bearer student-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].recipientId").doesNotExist())
        .andExpect(jsonPath("$.items[0].activityId").value("activity-1"))
        .andExpect(jsonPath("$.items[0].type").value("activity.review.approved"))
        .andExpect(jsonPath("$.items[0].read").value(false));
  }

  @Test
  void authenticatedUserMarksAllOwnActivityNotificationsRead() throws Exception {
    notifications.create("u-student", "activity-1", "activity.review.approved",
        "活动已发布", "校园编程工作坊已通过审核。");
    notifications.create("u-student", "activity-2", "activity.registration.registered",
        "活动报名成功", "已为你保留名额。");

    mockMvc.perform(post("/api/activity-notifications/read-all")
            .header("Authorization", "Bearer student-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].read").value(true))
        .andExpect(jsonPath("$.items[1].read").value(true));
  }

  @Test
  void authenticatedUserMarksOnlyOneActivityNotificationRead() throws Exception {
    var first = notifications.create("u-student", "activity-1", "activity.review.approved",
        "活动已发布", "校园编程工作坊已通过审核。");
    notifications.create("u-student", "activity-2", "activity.registration.registered",
        "报名成功", "已为你保留名额。");

    mockMvc.perform(post("/api/activity-notifications/{notificationId}/read", first.id())
            .header("Authorization", "Bearer student-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items[?(@.id == '" + first.id() + "')].read").value(true));
  }

  @Test
  void authenticatedUserCannotMarkAnotherUsersActivityNotificationRead() throws Exception {
    var otherNotification = notifications.create("u-other", "activity-2",
        "activity.registration.registered", "报名成功", "已为其他账号保留名额。");

    mockMvc.perform(post("/api/activity-notifications/{notificationId}/read", otherNotification.id())
            .header("Authorization", "Bearer student-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0));

    assertThat(notifications.findForRecipient("u-other").getFirst().readAt()).isNull();
  }
}
