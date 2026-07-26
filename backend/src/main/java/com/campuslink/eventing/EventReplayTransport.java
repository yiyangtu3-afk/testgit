package com.campuslink.eventing;

public interface EventReplayTransport {

  void replay(String topic, String key, ActivityRegistrationMessage event);
}
