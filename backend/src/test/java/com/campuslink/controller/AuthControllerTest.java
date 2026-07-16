package com.campuslink.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.CodeResponse;
import com.campuslink.dto.DemoDtos.CurrentUser;
import com.campuslink.dto.DemoDtos.LoginResponse;
import com.campuslink.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
  }

  @Test
  void createCodeReturnsDemoCode() throws Exception {
    when(authService.createCode("13800000001")).thenReturn(new CodeResponse("13800000001", "123456"));

    mockMvc.perform(post("/api/auth/code")
            .contentType("application/json")
            .content("{\"phone\":\"13800000001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.phone").value("13800000001"))
        .andExpect(jsonPath("$.code").value("123456"));
  }

  @Test
  void loginReturnsCurrentUser() throws Exception {
    when(authService.login("13800000001", "123456")).thenReturn(new LoginResponse(
        "demo-jwt-token",
        new CurrentUser("u-1001", "林一", "学生账号", "13800000001")));

    mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("demo-jwt-token"))
        .andExpect(jsonPath("$.user.id").value("u-1001"));
  }

  @Test
  void demoLoginReturnsSelectedDemoUser() throws Exception {
    when(authService.demoLogin("u-2003")).thenReturn(new LoginResponse(
        "demo-admin-token",
        new CurrentUser("u-2003", "教务管理员", "管理员账号", "13800000004")));

    mockMvc.perform(post("/api/auth/demo-login")
            .contentType("application/json")
            .content("{\"userId\":\"u-2003\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("demo-admin-token"))
        .andExpect(jsonPath("$.user.id").value("u-2003"));
  }

  @Test
  void logoutRevokesCurrentBearerSession() throws Exception {
    mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    verify(authService).logout("Bearer test-token");
  }
}
