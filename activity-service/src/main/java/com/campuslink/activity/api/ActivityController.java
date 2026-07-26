package com.campuslink.activity.api;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.campuslink.activity.api.ActivityDtos.CreateActivityRequest;
import com.campuslink.activity.api.ActivityDtos.ReviewActivityRequest;
import com.campuslink.activity.service.ActivityApplicationService;
import com.campuslink.activity.service.ActivityAuthService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActivityController {
  private final ActivityApplicationService activities;
  private final ActivityAuthService auth;
  public ActivityController(ActivityApplicationService activities, ActivityAuthService auth) { this.activities = activities; this.auth = auth; }
  @GetMapping("/api/activities")
  public List<ActivityView> published(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(required = false) String category, @RequestParam(required = false) String from, @RequestParam(required = false) String to) { auth.requireUser(authorization); return activities.published(category, date(from), date(to)); }
  @PostMapping("/api/activities") @ResponseStatus(HttpStatus.CREATED)
  public ActivityView create(@Valid @RequestBody CreateActivityRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) { return activities.create(auth.requireUser(authorization), request); }
  @GetMapping("/api/activities/managed")
  public List<ActivityView> managed(@RequestHeader(value = "Authorization", required = false) String authorization) { return activities.managed(auth.requireUser(authorization)); }
  @GetMapping("/api/admin/activities/pending")
  public List<ActivityView> pending(@RequestHeader(value = "Authorization", required = false) String authorization) { return activities.pending(auth.requireUser(authorization)); }
  @PostMapping("/api/admin/activities/{activityId}/reviews")
  public ActivityView review(@PathVariable String activityId, @Valid @RequestBody ReviewActivityRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) { return activities.review(auth.requireUser(authorization), activityId, request); }
  private LocalDate date(String raw) { if (raw == null || raw.isBlank()) return null; try { return LocalDate.parse(raw.trim()); } catch (DateTimeParseException exception) { throw new IllegalArgumentException("活动日期格式必须为 YYYY-MM-DD"); } }
}
