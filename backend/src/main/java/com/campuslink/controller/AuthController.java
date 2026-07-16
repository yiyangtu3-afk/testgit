package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.CodeResponse;
import com.campuslink.dto.DemoDtos.DemoLoginRequest;
import com.campuslink.dto.DemoDtos.LoginRequest;
import com.campuslink.dto.DemoDtos.LoginResponse;
import com.campuslink.dto.DemoDtos.PhoneRequest;
import com.campuslink.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/code")
  public CodeResponse createCode(@Valid @RequestBody PhoneRequest request) {
    return authService.createCode(request.phone());
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.phone(), request.code());
  }

  @PostMapping("/demo-login")
  public LoginResponse demoLogin(@Valid @RequestBody DemoLoginRequest request) {
    return authService.demoLogin(request.userId());
  }

  @PostMapping("/logout")
  public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
    authService.logout(authorization);
  }
}
