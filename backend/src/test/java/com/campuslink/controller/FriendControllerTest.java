package com.campuslink.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.FriendRequestResponse;
import com.campuslink.dto.DemoDtos.FriendRequestView;
import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FriendService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FriendControllerTest {

  private static final UserView ZHOU = new UserView(
      "u-2002", "周同学", "学生", "13800000003", "offline");

  @Mock
  private FriendService friendService;

  @Mock
  private AuthTokenService authTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new FriendController(friendService, authTokenService)).build();
  }

  @Test
  void friendsReturnsCurrentUsersContacts() throws Exception {
    when(friendService.friends("u-1001")).thenReturn(List.of(ZHOU));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(get("/api/friends").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("u-2002"))
        .andExpect(jsonPath("$[0].name").value("周同学"));
  }

  @Test
  void createFriendRequestReturnsStatus() throws Exception {
    when(friendService.createFriendRequest(eq("u-1001"), eq("u-2002")))
        .thenReturn(new FriendRequestResponse("u-2002", "pending"));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(post("/api/friends/requests")
            .header("Authorization", "Bearer test-token")
            .contentType("application/json")
            .content("{\"userId\":\"u-2002\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("u-2002"))
        .andExpect(jsonPath("$.status").value("pending"));
  }

  @Test
  void friendRequestsReturnsIncomingAndOutgoingRequests() throws Exception {
    when(friendService.friendRequests("u-1001")).thenReturn(List.of(
        new FriendRequestView("fr-1", "u-2002", "u-1001", "u-2002", "incoming", "pending", ZHOU)));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(get("/api/friends/requests").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].direction").value("incoming"))
        .andExpect(jsonPath("$[0].status").value("pending"));
  }

  @Test
  void acceptFriendRequestReturnsAcceptedRequest() throws Exception {
    when(friendService.acceptFriendRequest("fr-1", "u-1001")).thenReturn(
        new FriendRequestView("fr-1", "u-2002", "u-1001", "u-2002", "incoming", "accepted", ZHOU));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(post("/api/friends/requests/fr-1/accept").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("accepted"));
  }

  @Test
  void rejectFriendRequestReturnsRejectedRequest() throws Exception {
    when(friendService.rejectFriendRequest("fr-1", "u-1001")).thenReturn(
        new FriendRequestView("fr-1", "u-2002", "u-1001", "u-2002", "incoming", "rejected", ZHOU));
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");

    mockMvc.perform(post("/api/friends/requests/fr-1/reject").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("rejected"));
  }
}
