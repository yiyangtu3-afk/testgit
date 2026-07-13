package com.campuslink.controller;

import com.campuslink.dto.ActivityRegistrationDtos.ActivityMetricsView;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.AuthTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class ActivityOperationsAdminController {

  private final ActivityRegistrationService registrations;
  private final AuthTokenService authTokens;

  public ActivityOperationsAdminController(
      ActivityRegistrationService registrations,
      AuthTokenService authTokens) {
    this.registrations = registrations;
    this.authTokens = authTokens;
  }

  @GetMapping("/activity-metrics")
  public ActivityMetricsView metrics(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return registrations.metrics(authTokens.requireAdmin(authorization));
  }
}
