package com.campuslink.service;

import com.campuslink.dto.DemoDtos.MessageView;

public interface ChatRealtimeNotifier {

  void publishMessage(String peerId, MessageView message);

  void publishMessageWithdrawn(String peerId, MessageView message);
}
