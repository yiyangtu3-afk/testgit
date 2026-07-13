package com.campuslink.controller;

import com.campuslink.dto.ActivityRegistrationDtos.RegistrationView;
import com.campuslink.dto.ActivityRegistrationDtos.RosterView;
import com.campuslink.dto.ActivityRegistrationDtos.RosterEntryView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.ActivityRegistrationService;
import com.campuslink.service.AuthTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities/{activityId}/registrations")
public class ActivityRegistrationController {

  private final ActivityRegistrationService service;
  private final AuthTokenService authTokens;

  public ActivityRegistrationController(ActivityRegistrationService service, AuthTokenService authTokens) {
    this.service = service;
    this.authTokens = authTokens;
  }

  @GetMapping("/current")
  public ResponseEntity<RegistrationView> current(
      @PathVariable String activityId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity attendee = authTokens.requireUser(authorization);
    RegistrationView current = service.current(attendee, activityId);
    return current == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(current);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationView register(
      @PathVariable String activityId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return service.register(authTokens.requireUser(authorization), activityId);
  }

  @DeleteMapping("/current")
  public RegistrationView cancel(
      @PathVariable String activityId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return service.cancel(authTokens.requireUser(authorization), activityId);
  }

  @GetMapping("/roster")
  public RosterView roster(
      @PathVariable String activityId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return service.roster(authTokens.requireUser(authorization), activityId);
  }

  @PostMapping("/{registrationId}/check-in")
  public RosterEntryView checkIn(
      @PathVariable String activityId,
      @PathVariable String registrationId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return service.checkIn(authTokens.requireUser(authorization), activityId, registrationId);
  }
}
