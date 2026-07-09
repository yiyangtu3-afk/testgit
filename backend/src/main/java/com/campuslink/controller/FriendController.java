package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.FriendRequest;
import com.campuslink.dto.DemoDtos.FriendRequestResponse;
import com.campuslink.dto.DemoDtos.FriendRequestView;
import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FriendService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

  private final FriendService friendService;
  private final AuthTokenService authTokenService;

  public FriendController(FriendService friendService, AuthTokenService authTokenService) {
    this.friendService = friendService;
    this.authTokenService = authTokenService;
  }

  @GetMapping
  public List<UserView> friends(@RequestHeader(value = "Authorization", required = false) String authorization) {
    return friendService.friends(authTokenService.requireUserId(authorization));
  }

  @PostMapping("/requests")
  public FriendRequestResponse createFriendRequest(
      @Valid @RequestBody FriendRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return friendService.createFriendRequest(authTokenService.requireUserId(authorization), request.userId());
  }

  @GetMapping("/requests")
  public List<FriendRequestView> friendRequests(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return friendService.friendRequests(authTokenService.requireUserId(authorization));
  }

  @PostMapping("/requests/{requestId}/accept")
  public FriendRequestView acceptFriendRequest(
      @PathVariable String requestId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return friendService.acceptFriendRequest(requestId, authTokenService.requireUserId(authorization));
  }

  @PostMapping("/requests/{requestId}/reject")
  public FriendRequestView rejectFriendRequest(
      @PathVariable String requestId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return friendService.rejectFriendRequest(requestId, authTokenService.requireUserId(authorization));
  }
}
