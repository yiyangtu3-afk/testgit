package com.campuslink.service;

import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.ConversationPageView;
import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.PresenceResponse;
import com.campuslink.dto.DemoDtos.SendMessageRequest;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.repository.ChatRepository;
import com.campuslink.repository.FriendRepository;
import com.campuslink.repository.UserRepository;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final ChatRepository chatRepository;
  private final FriendRepository friendRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final AuditService auditService;
  private final ChatRealtimeNotifier chatRealtimeNotifier;

  public ChatService(
      ChatRepository chatRepository,
      FriendRepository friendRepository,
      UserRepository userRepository,
      UserService userService,
      AuditService auditService,
      ChatRealtimeNotifier chatRealtimeNotifier) {
    this.chatRepository = chatRepository;
    this.friendRepository = friendRepository;
    this.userRepository = userRepository;
    this.userService = userService;
    this.auditService = auditService;
    this.chatRealtimeNotifier = chatRealtimeNotifier;
  }

  public ConversationPageView messages(String peerId, String currentUserId, Long beforeId, int limit) {
    requireFriendship(currentUserId, peerId);
    int pageSize = Math.max(1, Math.min(limit, 50));
    List<MessageEntity> newestFirst = chatRepository.findMessagePage(peerId, currentUserId, beforeId, pageSize + 1);
    boolean hasMore = newestFirst.size() > pageSize;
    List<MessageEntity> page = newestFirst.stream().limit(pageSize).toList();
    Long nextBeforeId = hasMore ? page.getLast().id() : null;
    List<MessageView> messages = page.reversed().stream().map(DemoMapper::toMessageView).toList();
    if (beforeId == null && !page.isEmpty()) {
      chatRepository.markConversationRead(currentUserId, peerId, page.getFirst().id());
    }
    return new ConversationPageView(messages, hasMore, nextBeforeId);
  }

  public Map<String, Integer> unreadCounts(String currentUserId) {
    return chatRepository.unreadCounts(currentUserId);
  }

  public Map<String, MessageView> conversationPreviews(String currentUserId) {
    Map<String, MessageView> previews = new LinkedHashMap<>();
    for (String peerId : friendRepository.findFriendIdsForUser(currentUserId)) {
      chatRepository.findMessagePage(peerId, currentUserId, null, 1).stream()
          .findFirst()
          .map(DemoMapper::toMessageView)
          .ifPresent(message -> previews.put(peerId, message));
    }
    return previews;
  }

  public MessageView sendMessage(String peerId, String currentUserId, SendMessageRequest request) {
    requireFriendship(currentUserId, peerId);
    MessageEntity message = chatRepository.saveMessage(
        peerId,
        currentUserId,
        request.text(),
        DemoMapper.toAttachmentEntities(request.attachments()));
    String attachmentCopy = message.attachments().isEmpty() ? "" : "，包含 " + message.attachments().size() + " 个附件";
    auditService.addAudit("聊天", userService.userName(currentUserId) + "向" + userService.userName(peerId) + "发送消息" + attachmentCopy);
    MessageView view = DemoMapper.toMessageView(message);
    chatRealtimeNotifier.publishMessage(peerId, view);
    return view;
  }

  public MessageView withdrawMessage(String peerId, Long messageId, String currentUserId) {
    requireFriendship(currentUserId, peerId);
    MessageEntity message = chatRepository.findMessage(peerId, currentUserId, messageId)
        .orElseThrow(() -> new IllegalArgumentException("只能撤回自己发送的消息"));
    if (!currentUserId.equals(message.from())) {
      throw new IllegalArgumentException("只能撤回自己发送的消息");
    }
    chatRepository.withdrawMessage(peerId, currentUserId, messageId);
    MessageEntity withdrawn = chatRepository.findMessage(peerId, currentUserId, messageId).orElseThrow();
    auditService.addAudit("聊天", userService.userName(currentUserId) + "撤回消息 " + messageId);
    MessageView view = DemoMapper.toMessageView(withdrawn);
    chatRealtimeNotifier.publishMessageWithdrawn(peerId, view);
    return view;
  }

  public PresenceResponse updatePresence(String currentUserId, String presence) {
    userRepository.updatePresence(currentUserId, presence);
    auditService.addAudit("用户", userService.userName(currentUserId) + "切换为" + presenceLabel(presence));
    return new PresenceResponse(presence);
  }

  private String presenceLabel(String value) {
    return switch (value) {
      case "invisible" -> "隐身";
      case "offline" -> "离线";
      default -> "在线";
    };
  }

  private void requireFriendship(String currentUserId, String peerId) {
    if (!friendRepository.areFriends(currentUserId, peerId)) {
      throw new ForbiddenException("仅能与已建立好友关系的用户聊天");
    }
  }
}
