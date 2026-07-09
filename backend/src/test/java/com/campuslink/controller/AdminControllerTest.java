package com.campuslink.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.AuditEventView;
import com.campuslink.dto.DemoDtos.DeleteAuditEventsResponse;
import com.campuslink.dto.DemoDtos.DeleteModerationResponse;
import com.campuslink.dto.DemoDtos.ModerationItemView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.config.GlobalExceptionHandler;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.ForbiddenException;
import com.campuslink.service.AdminService;
import com.campuslink.service.AuditService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock
  private AdminService adminService;

  @Mock
  private AuditService auditService;

  @Mock
  private AuthTokenService authTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new AdminController(adminService, auditService, authTokenService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void metricsReturnsDashboardValues() throws Exception {
    Map<String, String> metrics = new LinkedHashMap<>();
    metrics.put("注册用户", "128");
    metrics.put("待审内容", "2");
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(adminService.metrics()).thenReturn(metrics);

    mockMvc.perform(get("/api/admin/metrics").header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.注册用户").value("128"))
        .andExpect(jsonPath("$.待审内容").value("2"));
  }

  @Test
  void moderationReturnsQueue() throws Exception {
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(adminService.moderationItems()).thenReturn(List.of(
        new ModerationItemView(
            "m-1",
            "post",
            1L,
            null,
            "动态：待审动态",
            "林一",
            "待审动态",
            "pending",
            "新动态待审核",
            "2026-07-06 09:30",
            "09:30")));

    mockMvc.perform(get("/api/admin/moderation").header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("m-1"))
        .andExpect(jsonPath("$[0].title").value("动态：待审动态"))
        .andExpect(jsonPath("$[0].submittedAt").value("2026-07-06 09:30"))
        .andExpect(jsonPath("$[0].status").value("pending"));
  }

  @Test
  void resolveModerationReturnsDecision() throws Exception {
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(adminService.resolveModeration("m-1", "approve", "教务管理员")).thenReturn(
        new ModerationItemView(
            "m-1",
            "post",
            1L,
            null,
            "动态：待审动态",
            "林一",
            "待审动态",
            "approved",
            "新动态待审核",
            "2026-07-06 09:30",
            "09:30"));

    mockMvc.perform(post("/api/admin/moderation/m-1/approve").header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("approved"));
  }

  @Test
  void deleteModerationItemsReturnsDeletedCount() throws Exception {
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(adminService.deleteModerationItems(List.of("m-1", "m-2"), "教务管理员"))
        .thenReturn(new DeleteModerationResponse(2));

    mockMvc.perform(delete("/api/admin/moderation")
            .header("Authorization", "Bearer admin-token")
            .contentType("application/json")
            .content("{\"itemIds\":[\"m-1\",\"m-2\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(2));
  }

  @Test
  void auditEventsReturnsRecentEvents() throws Exception {
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(auditService.auditEvents()).thenReturn(List.of(
        new AuditEventView("a-1", "09:30", "审核", "教务管理员通过林一的动态")));

    mockMvc.perform(get("/api/admin/audit-events").header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("a-1"))
        .andExpect(jsonPath("$[0].module").value("审核"));
  }

  @Test
  void deleteAuditEventsReturnsDeletedCount() throws Exception {
    when(authTokenService.requireAdmin("Bearer admin-token")).thenReturn(adminUser());
    when(auditService.deleteAuditEvents(List.of("a-1", "a-2")))
        .thenReturn(new DeleteAuditEventsResponse(2));

    mockMvc.perform(delete("/api/admin/audit-events")
            .header("Authorization", "Bearer admin-token")
            .contentType("application/json")
            .content("{\"eventIds\":[\"a-1\",\"a-2\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(2));
  }

  @Test
  void studentTokenCannotReadAdminMetrics() throws Exception {
    when(authTokenService.requireAdmin("Bearer student-token"))
        .thenThrow(new ForbiddenException("需要管理员账号"));

    mockMvc.perform(get("/api/admin/metrics").header("Authorization", "Bearer student-token"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("需要管理员账号"));
  }

  private UserEntity adminUser() {
    return new UserEntity("u-2003", "教务管理员", "管理员账号", "13800000004", "online");
  }
}
