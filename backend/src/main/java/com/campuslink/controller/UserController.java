package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final AuthTokenService authTokenService;

  public UserController(UserService userService, AuthTokenService authTokenService) {
    this.userService = userService;
    this.authTokenService = authTokenService;
  }

  @GetMapping
  public List<UserView> searchUsers(
      String keyword,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return userService.searchUsers(keyword, authTokenService.requireUserId(authorization));
  }
}
