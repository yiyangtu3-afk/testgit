package com.campuslink.repository;

import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.entity.ActivityRosterEntryEntity;
import com.campuslink.mapper.ActivityRegistrationMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisActivityRegistrationRepository implements ActivityRegistrationRepository {

  private final ActivityRegistrationMapper mapper;

  public MyBatisActivityRegistrationRepository(ActivityRegistrationMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ActivityRegistrationEntity findForUpdate(String activityId, String attendeeId) {
    return mapper.findForUpdate(activityId, attendeeId);
  }

  @Override
  public ActivityRegistrationEntity find(String activityId, String attendeeId) {
    return mapper.find(activityId, attendeeId);
  }

  @Override
  public ActivityRegistrationEntity findByIdForUpdate(String activityId, String registrationId) {
    return mapper.findByIdForUpdate(activityId, registrationId);
  }

  @Override
  public int countOccupied(String activityId) {
    return mapper.countOccupied(activityId);
  }

  @Override
  public int countWaitlisted(String activityId) {
    return mapper.countWaitlisted(activityId);
  }

  @Override
  public int countAllOccupied() {
    return mapper.countAllOccupied();
  }

  @Override
  public int countAllCheckedIn() {
    return mapper.countAllCheckedIn();
  }

  @Override
  public int queuePosition(String registrationId) {
    return mapper.queuePosition(registrationId);
  }

  @Override
  public ActivityRegistrationEntity findFirstWaitlistedForUpdate(String activityId) {
    return mapper.findFirstWaitlistedForUpdate(activityId);
  }

  @Override
  public ActivityRegistrationEntity create(String activityId, String attendeeId, String status) {
    String id = newId();
    mapper.insert(id, activityId, attendeeId, status);
    return mapper.findForUpdate(activityId, attendeeId);
  }

  @Override
  public int updateStatus(String registrationId, String status) {
    return mapper.updateStatus(registrationId, status);
  }

  @Override
  public void addEvent(String registrationId, String activityId, String attendeeId, String actorId,
      String eventType, String fromStatus, String toStatus) {
    mapper.insertEvent(newId(), registrationId, activityId, attendeeId, actorId, eventType,
        fromStatus, toStatus);
  }

  @Override
  public List<ActivityRegistrationEventEntity> findEvents(String activityId) {
    return mapper.findEvents(activityId);
  }

  @Override
  public List<ActivityRosterEntryEntity> findRoster(String activityId) {
    return mapper.findRoster(activityId);
  }

  private String newId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
