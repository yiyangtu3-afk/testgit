package com.campuslink.repository;

import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.entity.ActivityRosterEntryEntity;
import java.util.List;

public interface ActivityRegistrationRepository {

  ActivityRegistrationEntity findForUpdate(String activityId, String attendeeId);

  ActivityRegistrationEntity find(String activityId, String attendeeId);

  ActivityRegistrationEntity findByIdForUpdate(String activityId, String registrationId);

  int countOccupied(String activityId);

  int countWaitlisted(String activityId);

  int countAllOccupied();

  int countAllCheckedIn();

  int queuePosition(String registrationId);

  ActivityRegistrationEntity findFirstWaitlistedForUpdate(String activityId);

  ActivityRegistrationEntity create(String activityId, String attendeeId, String status);

  int updateStatus(String registrationId, String status);

  void addEvent(String registrationId, String activityId, String attendeeId, String actorId,
      String eventType, String fromStatus, String toStatus);

  List<ActivityRegistrationEventEntity> findEvents(String activityId);

  List<ActivityRosterEntryEntity> findRoster(String activityId);
}
