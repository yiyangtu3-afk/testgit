package com.campuslink.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.SocialNotificationService;
import com.campuslink.support.InMemorySocialNotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SocialNotificationControllerTest {

  private MockMvc mockMvc;
  private InMemorySocialNotificationRepository notifications;

  @BeforeEach
  void setUp() {
    UserEntity author = new UserEntity("u-author", "林一", "学生", "1", "online");
    UserEntity other = new UserEntity("u-other", "周同学", "学生", "2", "online");
    notifications = new InMemorySocialNotificationRepository();
    AuthSessionRepository sessions = new AuthSessionRepository() {
      public void save(String token, String userId) { }
      public Optional<String> findUserIdByToken(String token) {
        return "author-token".equals(token) ? Optional.of(author.id()) : Optional.empty();
      }
    };
    UserRepository users = new UserRepository() {
      public List<UserEntity> findAll() { return List.of(author, other); }
      public Optional<UserEntity> findById(String id) {
        return findAll().stream().filter(user -> user.id().equals(id)).findFirst();
      }
      public Optional<UserEntity> findByPhone(String phone) { return Optional.empty(); }
      public void updatePresence(String userId, String presence) { }
    };
    var service = new SocialNotificationService(notifications);
    mockMvc = MockMvcBuilders.standaloneSetup(new SocialNotificationController(
            service, new AuthTokenService(sessions, users)))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void authenticatedAuthorSeesOnlyOwnUnreadSocialNotifications() throws Exception {
    notifications.create("u-author", "u-other", "1", "social.post.liked",
        "动态收到新点赞", "周同学赞了你的动态。");
    notifications.create("u-other", "u-author", "2", "social.post.liked",
        "动态收到新点赞", "林一赞了你的动态。");

    mockMvc.perform(get("/api/social-notifications")
            .header("Authorization", "Bearer author-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].recipientId").doesNotExist())
        .andExpect(jsonPath("$.items[0].targetId").value("1"))
        .andExpect(jsonPath("$.items[0].type").value("social.post.liked"))
        .andExpect(jsonPath("$.items[0].read").value(false));
  }

  @Test
  void authenticatedAuthorMarksAllOwnSocialNotificationsRead() throws Exception {
    notifications.create("u-author", "u-other", "1", "social.post.liked",
        "动态收到新点赞", "周同学赞了你的动态。");

    mockMvc.perform(post("/api/social-notifications/read-all")
            .header("Authorization", "Bearer author-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0))
        .andExpect(jsonPath("$.items[0].read").value(true));
  }
}
