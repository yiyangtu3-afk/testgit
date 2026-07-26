package com.campuslink.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.dto.EventingDtos.EventingOperationsView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.eventing.EventingOperationsService;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.AuthTokenService;
import com.campuslink.support.InMemoryAuthSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EventingAdminControllerTest {

  @Test void permitsOnlyAdministratorsAndRequiresReplayConfirmation() throws Exception {
    UserEntity administrator = new UserEntity("admin", "教务管理员", "管理员", "1", "online");
    UserEntity student = new UserEntity("student", "林一", "学生", "2", "online");
    var authTokens = new AuthTokenService(new InMemoryAuthSessionRepository(), users(administrator, student));
    var operations = org.mockito.Mockito.mock(EventingOperationsService.class);
    org.mockito.Mockito.when(operations.operations()).thenReturn(
        new EventingOperationsView(new com.campuslink.dto.EventingDtos.EventingMetricsView(0, 0, 0, 0), List.of()));
    var mockMvc = MockMvcBuilders.standaloneSetup(new EventingAdminController(operations, authTokens))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    String adminAuthorization = "Bearer " + authTokens.issueToken(administrator.id());
    String studentAuthorization = "Bearer " + authTokens.issueToken(student.id());

    mockMvc.perform(get("/api/admin/eventing/operations").header("Authorization", adminAuthorization))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/eventing/operations").header("Authorization", studentAuthorization))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/admin/eventing/dead-letters/outbox/dead-1/replay")
            .header("Authorization", adminAuthorization))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/admin/eventing/dead-letters/outbox/dead-1/replay?confirm=true")
            .header("Authorization", adminAuthorization))
        .andExpect(status().isOk());

    verify(operations).replay("outbox", "dead-1", administrator.name());
  }

  private UserRepository users(UserEntity... users) {
    List<UserEntity> values = List.of(users);
    return new UserRepository() {
      @Override public List<UserEntity> findAll() { return values; }
      @Override public Optional<UserEntity> findById(String id) {
        return values.stream().filter(user -> user.id().equals(id)).findFirst();
      }
      @Override public Optional<UserEntity> findByPhone(String phone) { return Optional.empty(); }
      @Override public void updatePresence(String userId, String presence) { }
    };
  }
}
