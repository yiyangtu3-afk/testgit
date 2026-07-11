package com.campuslink.controller;

import com.campuslink.dto.ActivityDtos.ActivityView;
import com.campuslink.dto.ActivityDtos.CreateActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.ActivityService;
import com.campuslink.service.AuthTokenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

  private final ActivityService activityService;
  private final AuthTokenService authTokenService;

  public ActivityController(ActivityService activityService, AuthTokenService authTokenService) {
    this.activityService = activityService;
    this.authTokenService = authTokenService;
  }

  @GetMapping
  public List<ActivityView> published(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    authTokenService.requireUser(authorization);
    return activityService.published();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ActivityView create(
      @Valid @RequestBody CreateActivityRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity organizer = authTokenService.requireUser(authorization);
    return activityService.create(organizer, request);
  }
}
