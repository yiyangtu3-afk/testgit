package com.campuslink.service;

import com.campuslink.dto.ActivityRegistrationDtos.ActivityMetricsView;
import com.campuslink.dto.ActivityRegistrationDtos.CheckInCredentialView;
import com.campuslink.dto.ActivityRegistrationDtos.RegistrationView;
import com.campuslink.dto.ActivityRegistrationDtos.RosterEntryView;
import com.campuslink.dto.ActivityRegistrationDtos.RosterView;
import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ActivityRegistrationRepository;
import com.campuslink.repository.ActivityCheckInCredentialRepository;
import com.campuslink.repository.ActivityRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityRegistrationService {

  private final ActivityRepository activities;
  private final ActivityRegistrationRepository registrations;
  private final ActivityCheckInCredentialRepository credentials;
  private final ActivityNotificationService notifications;

  public ActivityRegistrationService(
      ActivityRepository activities,
      ActivityRegistrationRepository registrations,
      ActivityCheckInCredentialRepository credentials,
      ActivityNotificationService notifications) {
    this.activities = activities;
    this.registrations = registrations;
    this.credentials = credentials;
    this.notifications = notifications;
  }

  @Transactional
  public RegistrationView register(UserEntity attendee, String activityId) {
    requireStudent(attendee);
    ActivityEntity activity = activities.findByIdForUpdate(activityId)
        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
    requireOpen(activity);
    if (activity.organizerId().equals(attendee.id())) {
      throw new ForbiddenException("组织者不能报名自己的活动");
    }
    ActivityRegistrationEntity current = registrations.findForUpdate(activityId, attendee.id());
    if (current != null && ("registered".equals(current.status()) || "waitlisted".equals(current.status()))) {
      throw new ConflictException("你已报名该活动");
    }
    String status = registrations.countOccupied(activityId) < activity.capacity()
        ? "registered" : "waitlisted";
    ActivityRegistrationEntity registration = current == null
        ? registrations.create(activityId, attendee.id(), status)
        : reactivate(current, status);
    registrations.addEvent(registration.id(), activityId, attendee.id(), attendee.id(), status,
        current == null ? null : current.status(), status);
    if ("registered".equals(status) && registrations.countOccupied(activityId) == activity.capacity()) {
      activities.updateRegistrationStatus(activityId, "full");
    }
    int queuePosition = "waitlisted".equals(status)
        ? registrations.queuePosition(registration.id()) : 0;
    notifications.recordRegistrationResult(activity, attendee.id(), status, queuePosition);
    return view(registration, status, queuePosition);
  }

  public RegistrationView current(UserEntity attendee, String activityId) {
    requireStudent(attendee);
    ActivityRegistrationEntity registration = registrations.find(activityId, attendee.id());
    if (registration == null) {
      return null;
    }
    return view(registration, registration.status(),
        "waitlisted".equals(registration.status()) ? registrations.queuePosition(registration.id()) : 0);
  }

  public RosterView roster(UserEntity organizer, String activityId) {
    requireOrganizer(organizer);
    ActivityEntity activity = requireOwnedActivity(organizer, activityId, false);
    var entries = new ArrayList<RosterEntryView>();
    int waitlistPosition = 0;
    int registeredCount = 0;
    int waitlistedCount = 0;
    int checkedInCount = 0;
    for (var item : registrations.findRoster(activityId)) {
      int queuePosition = 0;
      if ("waitlisted".equals(item.status())) {
        waitlistedCount++;
        queuePosition = ++waitlistPosition;
      } else if ("checked_in".equals(item.status())) {
        checkedInCount++;
      } else if ("registered".equals(item.status())) {
        registeredCount++;
      }
      entries.add(new RosterEntryView(item.registrationId(), item.attendeeId(), item.attendeeName(),
          item.status(), queuePosition, item.registeredAt(), item.waitlistedAt(), item.checkedInAt()));
    }
    return new RosterView(activity.id(), activity.title(), activity.capacity(), registeredCount,
        waitlistedCount, checkedInCount, entries);
  }

  public ActivityMetricsView metrics(UserEntity administrator) {
    requireAdministrator(administrator);
    return new ActivityMetricsView(registrations.countAllOccupied(), registrations.countAllCheckedIn());
  }

  @Transactional
  public CheckInCredentialView credential(UserEntity attendee, String activityId) {
    requireStudent(attendee);
    ActivityRegistrationEntity registration = registrations.findForUpdate(activityId, attendee.id());
    if (registration == null || !"registered".equals(registration.status())) {
      throw new ConflictException("只有已报名且未签到的参与者可以领取签到凭证");
    }
    var existing = credentials.findByRegistrationId(registration.id());
    String code = newCredentialCode();
    if (existing == null) {
      credentials.create(registration.id(), hash(code));
    } else {
      credentials.replaceTokenHash(existing.id(), hash(code));
    }
    return new CheckInCredentialView(activityId, code);
  }

  @Transactional
  public RosterEntryView verifyCredential(UserEntity organizer, String activityId, String code) {
    requireOrganizer(organizer);
    requireCheckInOpen(requireOwnedActivity(organizer, activityId, true));
    var credential = credentials.findByTokenHashForUpdate(hash(code));
    if (credential == null) {
      throw new ConflictException("签到凭证无效");
    }
    ActivityRegistrationEntity registration =
        registrations.findByIdForUpdate(activityId, credential.registrationId());
    if (registration == null) {
      throw new ConflictException("签到凭证不属于当前活动");
    }
    return checkInRegistration(organizer, activityId, registration);
  }

  @Transactional
  public RosterEntryView checkIn(UserEntity organizer, String activityId, String registrationId) {
    requireOrganizer(organizer);
    requireCheckInOpen(requireOwnedActivity(organizer, activityId, true));
    ActivityRegistrationEntity registration = registrations.findByIdForUpdate(activityId, registrationId);
    return checkInRegistration(organizer, activityId, registration);
  }

  private RosterEntryView checkInRegistration(UserEntity organizer, String activityId,
      ActivityRegistrationEntity registration) {
    if (registration == null) {
      throw new IllegalArgumentException("报名记录不存在");
    }
    if ("checked_in".equals(registration.status())) {
      throw new ConflictException("该参与者已签到");
    }
    if (!"registered".equals(registration.status())) {
      throw new ConflictException("只有已报名参与者可以签到");
    }
    if (registrations.updateStatus(registration.id(), "checked_in") != 1) {
      throw new ConflictException("签到状态更新失败");
    }
    registrations.addEvent(registration.id(), activityId, registration.attendeeId(), organizer.id(),
        "checked_in", "registered", "checked_in");
    return registrations.findRoster(activityId).stream()
        .filter(item -> item.registrationId().equals(registration.id()))
        .findFirst()
        .map(item -> new RosterEntryView(item.registrationId(), item.attendeeId(), item.attendeeName(),
            item.status(), 0, item.registeredAt(), item.waitlistedAt(), item.checkedInAt()))
        .orElseThrow(() -> new IllegalStateException("签到结果读取失败"));
  }

  @Transactional
  public RegistrationView cancel(UserEntity attendee, String activityId) {
    requireStudent(attendee);
    ActivityEntity activity = activities.findByIdForUpdate(activityId)
        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
    ActivityRegistrationEntity registration = registrations.findForUpdate(activityId, attendee.id());
    if (registration == null || "cancelled".equals(registration.status())) {
      throw new ConflictException("没有可取消的报名记录");
    }
    if ("checked_in".equals(registration.status())) {
      throw new ConflictException("已签到的报名不能取消");
    }
    String previous = registration.status();
    registrations.updateStatus(registration.id(), "cancelled");
    registrations.addEvent(registration.id(), activityId, attendee.id(), attendee.id(), "cancelled", previous,
        "cancelled");
    if ("registered".equals(previous)) {
      ActivityRegistrationEntity next = registrations.findFirstWaitlistedForUpdate(activityId);
      if (next == null) {
        activities.updateRegistrationStatus(activityId, "published");
      } else {
        registrations.updateStatus(next.id(), "registered");
        registrations.addEvent(next.id(), activityId, next.attendeeId(), attendee.id(), "promoted",
            "waitlisted", "registered");
        notifications.recordPromotion(activity, next.attendeeId());
      }
    }
    return new RegistrationView(registration.id(), activityId, "cancelled", 0,
        registration.registeredAt(), registration.waitlistedAt());
  }

  private ActivityRegistrationEntity reactivate(ActivityRegistrationEntity registration, String status) {
    registrations.updateStatus(registration.id(), status);
    return registrations.findForUpdate(registration.activityId(), registration.attendeeId());
  }

  private void requireStudent(UserEntity attendee) {
    if (!attendee.role().contains("学生")) {
      throw new ForbiddenException("只有学生可以报名活动");
    }
  }

  private void requireOrganizer(UserEntity user) {
    if (!user.role().contains("教师") && !user.role().contains("社团负责人")) {
      throw new ForbiddenException("只有教师或社团负责人可以管理活动");
    }
  }

  private void requireAdministrator(UserEntity user) {
    if (!user.role().contains("管理员")) {
      throw new ForbiddenException("需要管理员账号查看活动指标");
    }
  }

  private ActivityEntity requireOwnedActivity(UserEntity organizer, String activityId, boolean lock) {
    ActivityEntity activity = (lock ? activities.findByIdForUpdate(activityId) : activities.findById(activityId))
        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
    if (!activity.organizerId().equals(organizer.id())) {
      throw new ForbiddenException("只能管理自己创建的活动");
    }
    return activity;
  }

  private void requireOpen(ActivityEntity activity) {
    if (!"published".equals(activity.status()) && !"full".equals(activity.status())) {
      throw new ConflictException("当前活动暂不接受报名");
    }
  }

  private void requireCheckInOpen(ActivityEntity activity) {
    if (!"published".equals(activity.status()) && !"full".equals(activity.status())) {
      throw new ConflictException("当前活动暂不支持签到");
    }
  }

  private RegistrationView view(ActivityRegistrationEntity registration, String status, int queuePosition) {
    return new RegistrationView(registration.id(), registration.activityId(), status, queuePosition,
        registration.registeredAt(), registration.waitlistedAt());
  }

  private String newCredentialCode() {
    byte[] bytes = new byte[24];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.strip().getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("无法生成签到凭证摘要", error);
    }
  }
}
