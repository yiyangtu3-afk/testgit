package com.campuslink.support;

import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.repository.ActivityRegistrationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InMemoryActivityRegistrationRepository implements ActivityRegistrationRepository {
  private final List<ActivityRegistrationEntity> registrations = new ArrayList<>();
  private final List<ActivityRegistrationEventEntity> events = new ArrayList<>();

  @Override public ActivityRegistrationEntity findForUpdate(String activityId, String attendeeId) {
    return find(activityId, attendeeId);
  }
  @Override public ActivityRegistrationEntity find(String activityId, String attendeeId) {
    return registrations.stream().filter(item -> item.activityId().equals(activityId)
        && item.attendeeId().equals(attendeeId)).findFirst().orElse(null);
  }
  @Override public int countOccupied(String activityId) {
    return (int) registrations.stream().filter(item -> item.activityId().equals(activityId)
        && ("registered".equals(item.status()) || "checked_in".equals(item.status()))).count();
  }
  @Override public int countWaitlisted(String activityId) {
    return (int) registrations.stream().filter(item -> item.activityId().equals(activityId)
        && "waitlisted".equals(item.status())).count();
  }
  @Override public int queuePosition(String registrationId) {
    var current = registrations.stream().filter(item -> item.id().equals(registrationId)).findFirst().orElseThrow();
    return (int) registrations.stream().filter(item -> item.activityId().equals(current.activityId())
        && "waitlisted".equals(item.status()))
        .filter(item -> item.waitlistedAt().isBefore(current.waitlistedAt())
            || item.waitlistedAt().equals(current.waitlistedAt()) && item.id().compareTo(current.id()) <= 0)
        .count();
  }
  @Override public ActivityRegistrationEntity findFirstWaitlistedForUpdate(String activityId) {
    return registrations.stream().filter(item -> item.activityId().equals(activityId)
        && "waitlisted".equals(item.status())).min(Comparator.comparing(ActivityRegistrationEntity::waitlistedAt))
        .orElse(null);
  }
  @Override public ActivityRegistrationEntity create(String activityId, String attendeeId, String status) {
    LocalDateTime now = LocalDateTime.of(2026, 7, 11, 9, registrations.size());
    var item = new ActivityRegistrationEntity("registration-" + (registrations.size() + 1), activityId,
        attendeeId, status, "registered".equals(status) ? now : null,
        "waitlisted".equals(status) ? now : null, null, now);
    registrations.add(item);
    return item;
  }
  @Override public int updateStatus(String registrationId, String status) {
    for (int index = 0; index < registrations.size(); index++) {
      var current = registrations.get(index);
      if (current.id().equals(registrationId)) {
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 10, index);
        registrations.set(index, new ActivityRegistrationEntity(current.id(), current.activityId(),
            current.attendeeId(), status, "registered".equals(status) ? now : current.registeredAt(),
            "waitlisted".equals(status) ? now : current.waitlistedAt(),
            "cancelled".equals(status) ? now : null, current.createdAt()));
        return 1;
      }
    }
    return 0;
  }
  @Override public void addEvent(String registrationId, String activityId, String attendeeId,
      String actorId, String eventType, String fromStatus, String toStatus) {
    events.add(new ActivityRegistrationEventEntity("event-" + (events.size() + 1), registrationId,
        activityId, attendeeId, actorId, eventType, fromStatus, toStatus,
        LocalDateTime.of(2026, 7, 11, 11, events.size())));
  }
  @Override public List<ActivityRegistrationEventEntity> findEvents(String activityId) {
    return events.stream().filter(item -> item.activityId().equals(activityId)).toList();
  }
}
