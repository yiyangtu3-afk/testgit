package com.campuslink.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.CampusLinkApplication;
import com.campuslink.service.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
@Rollback
class SecurityConfigIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired AuthTokenService authTokens;

  @Test
  void securityChainRejectsUnauthenticatedApiRequestsAsJson() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("请先登录"));
  }

  @Test
  void securityChainRejectsInvalidBearerTokensWithoutReachingControllers() throws Exception {
    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer not-a-signed-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("登录令牌无效"));
  }

  @Test
  void securityChainPermitsDemoLoginWithoutBearerToken() throws Exception {
    mockMvc.perform(post("/api/auth/demo-login")
            .contentType("application/json")
            .content("{\"userId\":\"u-1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void securityChainAcceptsSignedSessionTokenAndBlocksStudentAdminAccess() throws Exception {
    String studentToken = authTokens.issueToken("u-1001");

    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + studentToken))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/admin/metrics")
            .header("Authorization", "Bearer " + studentToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("需要管理员账号"));
  }
}
