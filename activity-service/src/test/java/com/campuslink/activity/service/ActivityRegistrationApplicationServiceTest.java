package com.campuslink.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campuslink.activity.domain.ActivityRecord;
import com.campuslink.activity.domain.RegistrationRecord;
import com.campuslink.activity.domain.UserDirectoryEntry;
import com.campuslink.activity.mapper.ActivityMapper;
import com.campuslink.activity.mapper.CheckInCredentialMapper;
import com.campuslink.activity.mapper.OutboxEventMapper;
import com.campuslink.activity.mapper.RegistrationMapper;
import com.campuslink.activity.mapper.UserDirectoryMapper;
import com.campuslink.activity.ratelimit.CheckInRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ActivityRegistrationApplicationServiceTest {

  @Test
  void putsStudentOnWaitlistAndWritesAKeyedOutboxEvent() {
    var activities = mock(ActivityMapper.class);
    var registrations = mock(RegistrationMapper.class);
    var outbox = mock(OutboxEventMapper.class);
    var service = service(activities, registrations, outbox);
    var activity = activity("full");
    var saved = registration("registration-1", "waitlisted");
    when(activities.findForUpdate("activity-1")).thenReturn(activity);
    when(registrations.occupied("activity-1")).thenReturn(1);
    when(registrations.findForUpdate("activity-1", "student-1")).thenReturn(null, saved);
    when(registrations.queuePosition("registration-1")).thenReturn(2);

    var result = service.register(student(), "activity-1");

    assertThat(result.status()).isEqualTo("waitlisted");
    assertThat(result.queuePosition()).isEqualTo(2);
    verify(registrations).insert(any(), eq("activity-1"), eq("student-1"), eq("waitlisted"));
    verify(registrations).event(any(), eq("registration-1"), eq("activity-1"), eq("student-1"),
        eq("student-1"), eq("waitlisted"), eq(null), eq("waitlisted"));
    verify(outbox).insert(any(), eq("activity-registration"), eq("activity-1"),
        eq("activity.registration.waitlisted.v1"), any());
  }

  @Test
  void cancellationPromotesTheEarliestWaitlistedStudentAndEmitsBothEvents() {
    var activities = mock(ActivityMapper.class);
    var registrations = mock(RegistrationMapper.class);
    var outbox = mock(OutboxEventMapper.class);
    var service = service(activities, registrations, outbox);
    var current = registration("registration-1", "registered");
    var next = new RegistrationRecord("registration-2", "activity-1", "student-2", "waitlisted",
        null, LocalDateTime.of(2026, 8, 1, 9, 0), null, null, LocalDateTime.now());
    when(activities.findForUpdate("activity-1")).thenReturn(activity("full"));
    when(registrations.findForUpdate("activity-1", "student-1")).thenReturn(current);
    when(registrations.firstWaitlisted("activity-1")).thenReturn(next);

    var result = service.cancel(student(), "activity-1");

    assertThat(result.status()).isEqualTo("cancelled");
    verify(registrations).status("registration-1", "cancelled");
    verify(registrations).status("registration-2", "registered");
    verify(registrations).event(any(), eq("registration-2"), eq("activity-1"), eq("student-2"),
        eq("student-1"), eq("promoted"), eq("waitlisted"), eq("registered"));
  }

  @Test
  void credentialIssueChecksTheStudentActivityRateLimit() {
    var activities = mock(ActivityMapper.class);
    var registrations = mock(RegistrationMapper.class);
    var outbox = mock(OutboxEventMapper.class);
    var credentials = mock(CheckInCredentialMapper.class);
    var rateLimiter = mock(CheckInRateLimiter.class);
    UserDirectoryMapper users = id -> new UserDirectoryEntry(id, id, "学生");
    var service = new ActivityRegistrationApplicationService(activities, registrations, credentials,
        users, outbox, new ObjectMapper().findAndRegisterModules(),
        (ApplicationEventPublisher) event -> {}, rateLimiter);
    var registered = registration("registration-1", "registered");
    when(registrations.findForUpdate("activity-1", "student-1")).thenReturn(registered);
    when(credentials.byRegistration("registration-1")).thenReturn(null);

    service.credential(student(), "activity-1");

    verify(rateLimiter).acquireCredentialIssue("student-1", "activity-1");
  }

  private ActivityRegistrationApplicationService service(
      ActivityMapper activities, RegistrationMapper registrations, OutboxEventMapper outbox) {
    return service(activities, registrations, outbox, mock(CheckInRateLimiter.class));
  }

  private ActivityRegistrationApplicationService service(
      ActivityMapper activities, RegistrationMapper registrations, OutboxEventMapper outbox,
      CheckInRateLimiter rateLimiter) {
    UserDirectoryMapper users = id -> new UserDirectoryEntry(id, id, "学生");
    return new ActivityRegistrationApplicationService(activities, registrations,
        mock(CheckInCredentialMapper.class), users, outbox,
        new ObjectMapper().findAndRegisterModules(), (ApplicationEventPublisher) event -> {},
        rateLimiter);
  }

  private UserDirectoryEntry student() {
    return new UserDirectoryEntry("student-1", "林一", "学生");
  }

  private ActivityRecord activity(String status) {
    return new ActivityRecord("activity-1", "校园编程赛", "描述", "技术", "创新中心",
        LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 11, 0), 1,
        "teacher-1", status, "approved", null, "admin-1", null, LocalDateTime.now());
  }

  private RegistrationRecord registration(String id, String status) {
    return new RegistrationRecord(id, "activity-1", "student-1", status,
        LocalDateTime.now(), null, null, null, LocalDateTime.now());
  }
}
