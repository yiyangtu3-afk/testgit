package com.campuslink.repository;

import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.ActivityRegistrationEventEntity;
import java.util.List;

public interface ActivityRegistrationRepository {

  ActivityRegistrationEntity findForUpdate(String activityId, String attendeeId);

  ActivityRegistrationEntity find(String activityId, String attendeeId);

  int countOccupied(String activityId);

  int countWaitlisted(String activityId);

  int queuePosition(String registrationId);

  ActivityRegistrationEntity findFirstWaitlistedForUpdate(String activityId);

  ActivityRegistrationEntity create(String activityId, String attendeeId, String status);

  int updateStatus(String registrationId, String status);

  void addEvent(String registrationId, String activityId, String attendeeId, String actorId,
      String eventType, String fromStatus, String toStatus);

  List<ActivityRegistrationEventEntity> findEvents(String activityId);
}
