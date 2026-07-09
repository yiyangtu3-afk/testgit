package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import java.util.List;
import java.util.Optional;

public interface ChatRepository {

  List<MessageEntity> findMessages(String peerId, String currentUserId);

  MessageEntity saveMessage(String peerId, String fromUserId, String body, List<AttachmentEntity> attachments);

  Optional<MessageEntity> findMessage(String peerId, String currentUserId, Long messageId);

  void withdrawMessage(String peerId, String currentUserId, Long messageId);
}
