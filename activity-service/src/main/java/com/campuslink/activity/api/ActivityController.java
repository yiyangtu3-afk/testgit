package com.campuslink.activity.api;

import com.campuslink.activity.api.ActivityDtos.ActivityView;
import com.campuslink.activity.api.ActivityDtos.CreateActivityRequest;
import com.campuslink.activity.api.ActivityDtos.ReviewActivityRequest;
import com.campuslink.activity.service.ActivityApplicationService;
import com.campuslink.activity.service.ActivityAuthService;
import com.campuslink.activity.service.ActivityRegistrationApplicationService;
import com.campuslink.activity.idempotency.RegistrationIdempotency;
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
  private final ActivityRegistrationApplicationService registrations;
  private final RegistrationIdempotency registrationIdempotency;
  public ActivityController(ActivityApplicationService activities, ActivityAuthService auth, ActivityRegistrationApplicationService registrations, RegistrationIdempotency registrationIdempotency) { this.activities = activities; this.auth = auth; this.registrations = registrations; this.registrationIdempotency = registrationIdempotency; }
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
  @GetMapping("/api/activities/{activityId}/registrations/current")
  public org.springframework.http.ResponseEntity<ActivityDtos.RegistrationView> current(@PathVariable String activityId,@RequestHeader(value="Authorization",required=false) String authorization){var result=registrations.current(auth.requireUser(authorization),activityId);return result==null?org.springframework.http.ResponseEntity.noContent().build():org.springframework.http.ResponseEntity.ok(result);}
  @PostMapping("/api/activities/{activityId}/registrations") @ResponseStatus(HttpStatus.CREATED)
  public ActivityDtos.RegistrationView register(@PathVariable String activityId,@RequestHeader(value="Authorization",required=false) String authorization,@RequestHeader(value="Idempotency-Key",required=false) String idempotencyKey){var user=auth.requireUser(authorization);return idempotencyKey==null||idempotencyKey.isBlank()?registrations.register(user,activityId):registrationIdempotency.execute(user.id(),activityId,idempotencyKey,()->registrations.register(user,activityId));}
  @org.springframework.web.bind.annotation.DeleteMapping("/api/activities/{activityId}/registrations/current")
  public ActivityDtos.RegistrationView cancel(@PathVariable String activityId,@RequestHeader(value="Authorization",required=false) String authorization){return registrations.cancel(auth.requireUser(authorization),activityId);}
  @GetMapping("/api/activities/{activityId}/registrations/roster")
  public ActivityDtos.RosterView roster(@PathVariable String activityId,@RequestHeader(value="Authorization",required=false) String authorization){return registrations.roster(auth.requireUser(authorization),activityId);}
  @PostMapping("/api/activities/{activityId}/registrations/current/check-in-credential")
  public ActivityDtos.CheckInCredentialView credential(@PathVariable String activityId,@RequestHeader(value="Authorization",required=false) String authorization){return registrations.credential(auth.requireUser(authorization),activityId);}
  @PostMapping("/api/activities/{activityId}/registrations/check-in-credential")
  public ActivityDtos.RosterEntryView verifyCredential(@PathVariable String activityId,@Valid @RequestBody ActivityDtos.VerifyCheckInCredentialRequest request,@RequestHeader(value="Authorization",required=false) String authorization){return registrations.verifyCredential(auth.requireUser(authorization),activityId,request.code());}
  @PostMapping("/api/activities/{activityId}/registrations/{registrationId}/check-in")
  public ActivityDtos.RosterEntryView checkIn(@PathVariable String activityId,@PathVariable String registrationId,@RequestHeader(value="Authorization",required=false) String authorization){return registrations.checkIn(auth.requireUser(authorization),activityId,registrationId);}
  @GetMapping("/api/admin/activity-metrics")
  public ActivityDtos.ActivityMetricsView metrics(@RequestHeader(value="Authorization",required=false) String authorization){return registrations.metrics(auth.requireUser(authorization));}
  private LocalDate date(String raw) { if (raw == null || raw.isBlank()) return null; try { return LocalDate.parse(raw.trim()); } catch (DateTimeParseException exception) { throw new IllegalArgumentException("活动日期格式必须为 YYYY-MM-DD"); } }
}
