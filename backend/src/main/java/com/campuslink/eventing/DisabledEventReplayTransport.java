package com.campuslink.eventing;

import com.campuslink.service.ConflictException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!eventing")
public class DisabledEventReplayTransport implements EventReplayTransport {

  @Override
  public void replay(String topic, String key, ActivityRegistrationMessage event) {
    throw new ConflictException("Kafka 事件功能当前未启用，无法重放消费者死信事件");
  }
}
