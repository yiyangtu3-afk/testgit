package com.campuslink.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock
  private UserService userService;

  @Mock
  private AuthTokenService authTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, authTokenService)).build();
  }

  @Test
  void searchUsersReturnsMatchingUsers() throws Exception {
    when(userService.searchUsers("老师", "u-1001")).thenReturn(List.of(
        new UserView("u-2001", "陈老师", "教师", "13800000002", "online")));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(get("/api/users")
            .param("keyword", "老师")
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("u-2001"))
        .andExpect(jsonPath("$[0].name").value("陈老师"));
  }
}
