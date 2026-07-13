package com.campuslink.service;

import com.campuslink.dto.ActivityRegistrationDtos.RegistrationView;
import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ActivityRegistrationRepository;
import com.campuslink.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityRegistrationService {

  private final ActivityRepository activities;
  private final ActivityRegistrationRepository registrations;

  public ActivityRegistrationService(
      ActivityRepository activities, ActivityRegistrationRepository registrations) {
    this.activities = activities;
    this.registrations = registrations;
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
    return view(registration, status,
        "waitlisted".equals(status) ? registrations.queuePosition(registration.id()) : 0);
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

  @Transactional
  public RegistrationView cancel(UserEntity attendee, String activityId) {
    requireStudent(attendee);
    ActivityEntity activity = activities.findByIdForUpdate(activityId)
        .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
    ActivityRegistrationEntity registration = registrations.findForUpdate(activityId, attendee.id());
    if (registration == null || "cancelled".equals(registration.status())) {
      throw new ConflictException("没有可取消的报名记录");
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

  private void requireOpen(ActivityEntity activity) {
    if (!"published".equals(activity.status()) && !"full".equals(activity.status())) {
      throw new ConflictException("当前活动暂不接受报名");
    }
  }

  private RegistrationView view(ActivityRegistrationEntity registration, String status, int queuePosition) {
    return new RegistrationView(registration.id(), registration.activityId(), status, queuePosition,
        registration.registeredAt(), registration.waitlistedAt());
  }
}
