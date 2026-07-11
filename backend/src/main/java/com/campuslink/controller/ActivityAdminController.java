package com.campuslink.controller;

import com.campuslink.dto.ActivityDtos.ActivityView;
import com.campuslink.dto.ActivityDtos.ReviewActivityRequest;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.service.ActivityService;
import com.campuslink.service.AuthTokenService;
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
@RequestMapping("/api/admin/activities")
public class ActivityAdminController {

  private final ActivityService activityService;
  private final AuthTokenService authTokenService;

  public ActivityAdminController(ActivityService activityService, AuthTokenService authTokenService) {
    this.activityService = activityService;
    this.authTokenService = authTokenService;
  }

  @GetMapping("/pending")
  public List<ActivityView> pending(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity reviewer = authTokenService.requireUser(authorization);
    return activityService.pending(reviewer);
  }

  @PostMapping("/{activityId}/reviews")
  public ActivityView review(
      @PathVariable String activityId,
      @Valid @RequestBody ReviewActivityRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    UserEntity reviewer = authTokenService.requireUser(authorization);
    return activityService.review(reviewer, activityId, request);
  }
}
