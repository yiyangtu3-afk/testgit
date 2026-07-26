package com.campuslink.eventing;

import com.campuslink.entity.ActivityRegistrationEventEntity;

public interface ActivityRegistrationEventOutbox {

  void enqueue(ActivityRegistrationEventEntity event, ActivityRegistrationEventContext context);
}
