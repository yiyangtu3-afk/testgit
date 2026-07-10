package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.mapper.ChatMapper;
import com.campuslink.mapper.ChatMapper.AttachmentRow;
import com.campuslink.mapper.ChatMapper.MessageRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisChatRepository implements ChatRepository {

  private final ChatMapper chatMapper;
  private final AtomicLong messageIds = new AtomicLong(System.currentTimeMillis() * 1000);

  public MyBatisChatRepository(ChatMapper chatMapper) {
    this.chatMapper = chatMapper;
  }

  @Override
  public List<MessageEntity> findMessagePage(String peerId, String currentUserId, Long beforeId, int limit) {
    return chatMapper.findMessagePage(peerId, currentUserId, beforeId, limit).stream()
        .map(this::toMessageEntity)
        .toList();
  }

  @Override
  public void markConversationRead(String currentUserId, String peerId, Long lastReadMessageId) {
    chatMapper.upsertConversationRead(currentUserId, peerId, String.valueOf(lastReadMessageId));
  }

  @Override
  public Map<String, Integer> unreadCounts(String currentUserId) {
    return chatMapper.unreadCounts(currentUserId).stream()
        .collect(java.util.stream.Collectors.toMap(
            ChatMapper.UnreadCountRow::peerId,
            ChatMapper.UnreadCountRow::count));
  }

  @Override
  @Transactional
  public MessageEntity saveMessage(
      String peerId,
      String fromUserId,
      String body,
      List<AttachmentEntity> attachments) {
    long messageId = messageIds.incrementAndGet();
    String messageKey = String.valueOf(messageId);
    chatMapper.insertMessage(messageKey, peerId, fromUserId, body);
    for (AttachmentEntity attachment : attachments) {
      chatMapper.insertAttachment(
          attachment.id(),
          messageKey,
          attachment.name(),
          attachment.size(),
          attachment.type(),
          attachment.kind());
    }
    return findMessage(peerId, fromUserId, messageId).orElseThrow();
  }

  @Override
  public Optional<MessageEntity> findMessage(String peerId, String currentUserId, Long messageId) {
    MessageRow row = chatMapper.findMessage(peerId, currentUserId, String.valueOf(messageId));
    return Optional.ofNullable(row).map(this::toMessageEntity);
  }

  @Override
  public void withdrawMessage(String peerId, String currentUserId, Long messageId) {
    chatMapper.withdrawMessage(peerId, currentUserId, String.valueOf(messageId));
  }

  private MessageEntity toMessageEntity(MessageRow row) {
    return new MessageEntity(
        row.id(),
        row.fromUserId(),
        row.body(),
        row.time(),
        "withdrawn".equals(row.status()),
        findAttachments(row.id()));
  }

  private List<AttachmentEntity> findAttachments(Long messageId) {
    return chatMapper.findAttachments(String.valueOf(messageId)).stream()
        .map(this::toAttachmentEntity)
        .toList();
  }

  private AttachmentEntity toAttachmentEntity(AttachmentRow row) {
    return new AttachmentEntity(
        row.id(),
        row.fileName(),
        row.fileSize(),
        row.mimeType(),
        row.displayKind());
  }
}
