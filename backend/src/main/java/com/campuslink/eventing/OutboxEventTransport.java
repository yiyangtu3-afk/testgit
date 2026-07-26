package com.campuslink.eventing;

public interface OutboxEventTransport {

  void publish(ActivityRegistrationMessage event);
}
