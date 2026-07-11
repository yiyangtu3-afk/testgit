package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.DemoDtos.AttachmentRequest;
import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.SendMessageRequest;
import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ChatRepository;
import com.campuslink.repository.FriendRepository;
import com.campuslink.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  private final InMemoryChatRepository chat = new InMemoryChatRepository();
  private final RecordingChatRealtimeNotifier chatRealtime = new RecordingChatRealtimeNotifier();
  private final ChatService chatService = new ChatService(
      chat,
      new InMemoryFriendRepository(List.of(List.of("u-1001", "u-2001"))),
      new InMemoryUserRepository(List.of(
          new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
          new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online"))),
      new UserService(new InMemoryUserRepository(List.of(
          new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
          new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online")))),
      new AuditService(new TestAuditRepository()),
      chatRealtime);

  @Test
  void sendMessagePersistsBodyAndAttachments() {
    MessageView message = chatService.sendMessage(
        "u-2001",
        "u-1001",
        new SendMessageRequest("老师好", List.of(
            new AttachmentRequest("att-1", "demo.txt", 128, "text/plain", "document"))));

    assertThat(message.text()).isEqualTo("老师好");
    assertThat(message.attachments()).extracting("name").containsExactly("demo.txt");
    assertThat(chat.findMessages("u-2001", "u-1001")).hasSize(1);
    assertThat(chatRealtime.events).extracting("peerId").containsExactly("u-2001");
  }

  @Test
  void messagesReturnsBothSidesOfConversation() {
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("老师好", List.of()));
    chatService.sendMessage("u-1001", "u-2001", new SendMessageRequest("收到", List.of()));

    List<MessageView> studentView = chatService.messages("u-2001", "u-1001", null, 30).messages();
    List<MessageView> teacherView = chatService.messages("u-1001", "u-2001", null, 30).messages();

    assertThat(studentView).extracting(MessageView::text).containsExactly("老师好", "收到");
    assertThat(teacherView).extracting(MessageView::text).containsExactly("老师好", "收到");
  }

  @Test
  void messagesPaginatesNewestFirstAndMarksLatestPageRead() {
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("第一条", List.of()));
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("第二条", List.of()));
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("第三条", List.of()));

    var latest = chatService.messages("u-2001", "u-1001", null, 2);
    var older = chatService.messages("u-2001", "u-1001", latest.nextBeforeId(), 2);

    assertThat(latest.messages()).extracting(MessageView::text).containsExactly("第二条", "第三条");
    assertThat(latest.hasMore()).isTrue();
    assertThat(latest.nextBeforeId()).isEqualTo(2L);
    assertThat(older.messages()).extracting(MessageView::text).containsExactly("第一条");
    assertThat(older.hasMore()).isFalse();
    assertThat(chat.lastReadMessageId).isEqualTo(3L);
  }

  @Test
  void conversationPreviewsReturnsLatestMessageWithoutMarkingItRead() {
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("第一条", List.of()));
    chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("最新消息", List.of()));

    Map<String, MessageView> previews = chatService.conversationPreviews("u-1001");

    assertThat(previews.get("u-2001").text()).isEqualTo("最新消息");
    assertThat(chat.lastReadMessageId).isNull();
  }

  @Test
  void withdrawMarksCurrentUsersMessageDeleted() {
    MessageView message = chatService.sendMessage("u-2001", "u-1001", new SendMessageRequest("可撤回", List.of()));

    MessageView withdrawn = chatService.withdrawMessage("u-2001", message.id(), "u-1001");

    assertThat(withdrawn.deleted()).isTrue();
    assertThat(chatRealtime.events).extracting("type").containsExactly("message.created", "message.withdrawn");
    assertThat(chatRealtime.events.getLast().message().id()).isEqualTo(message.id());
  }

  @Test
  void withdrawRejectsPeerMessage() {
    MessageEntity message = chat.saveMessage("u-1001", "u-2001", "来自老师", List.of());

    assertThatThrownBy(() -> chatService.withdrawMessage("u-2001", message.id(), "u-1001"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("只能撤回自己发送的消息");
  }

  @Test
  void messagesRejectsNonFriendPeer() {
    assertThatThrownBy(() -> chatService.messages("u-2002", "u-1001", null, 30))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("仅能与已建立好友关系的用户聊天");
  }

  @Test
  void sendMessageRejectsNonFriendPeer() {
    assertThatThrownBy(() -> chatService.sendMessage("u-2002", "u-1001", new SendMessageRequest("越权消息", List.of())))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("仅能与已建立好友关系的用户聊天");
    assertThat(chat.findMessages("u-2002", "u-1001")).isEmpty();
  }

  @Test
  void withdrawRejectsNonFriendPeer() {
    assertThatThrownBy(() -> chatService.withdrawMessage("u-2002", 1L, "u-1001"))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("仅能与已建立好友关系的用户聊天");
  }

  private static final class RecordingChatRealtimeNotifier implements ChatRealtimeNotifier {

    private final List<ChatRealtimeEvent> events = new ArrayList<>();

    @Override
    public void publishMessage(String peerId, MessageView message) {
      events.add(new ChatRealtimeEvent("message.created", peerId, message));
    }

    @Override
    public void publishMessageWithdrawn(String peerId, MessageView message) {
      events.add(new ChatRealtimeEvent("message.withdrawn", peerId, message));
    }
  }

  private record ChatRealtimeEvent(String type, String peerId, MessageView message) {
  }

  private static final class InMemoryChatRepository implements ChatRepository {

    private final List<MessageEntity> messages = new ArrayList<>();
    private final Map<Long, String> messagePeers = new HashMap<>();
    private Long lastReadMessageId;

    @Override
    public List<MessageEntity> findMessagePage(String peerId, String currentUserId, Long beforeId, int limit) {
      return messages.stream()
          .filter(message -> isConversationMessage(message, peerId, currentUserId))
          .filter(message -> beforeId == null || message.id() < beforeId)
          .sorted((first, second) -> Long.compare(second.id(), first.id()))
          .limit(limit)
          .toList();
    }

    public List<MessageEntity> findMessages(String peerId, String currentUserId) {
      return messages.stream()
          .filter(message -> isConversationMessage(message, peerId, currentUserId))
          .toList();
    }

    @Override
    public void markConversationRead(String currentUserId, String peerId, Long lastReadMessageId) {
      this.lastReadMessageId = lastReadMessageId;
    }

    @Override
    public Map<String, Integer> unreadCounts(String currentUserId) {
      return Map.of();
    }

    @Override
    public MessageEntity saveMessage(
        String peerId,
        String fromUserId,
        String body,
        List<AttachmentEntity> attachments) {
      MessageEntity message = new MessageEntity((long) messages.size() + 1, fromUserId, body, "09:30", false, attachments);
      messages.add(message);
      messagePeers.put(message.id(), peerId);
      return message;
    }

    @Override
    public Optional<MessageEntity> findMessage(String peerId, String currentUserId, Long messageId) {
      return messages.stream()
          .filter(message -> message.id().equals(messageId))
          .filter(message -> isConversationMessage(message, peerId, currentUserId))
          .findFirst();
    }

    @Override
    public void withdrawMessage(String peerId, String currentUserId, Long messageId) {
      for (int index = 0; index < messages.size(); index++) {
        MessageEntity message = messages.get(index);
        if (message.id().equals(messageId) && isConversationMessage(message, peerId, currentUserId)) {
          messages.set(index, new MessageEntity(
              message.id(),
              message.from(),
              message.text(),
              message.time(),
              true,
              message.attachments()));
        }
      }
    }

    private boolean isConversationMessage(MessageEntity message, String peerId, String currentUserId) {
      return (message.from().equals(currentUserId) && messagePeer(message).equals(peerId))
          || (message.from().equals(peerId) && messagePeer(message).equals(currentUserId));
    }

    private String messagePeer(MessageEntity message) {
      return messagePeers.get(message.id());
    }
  }

  private static final class InMemoryUserRepository implements UserRepository {

    private final List<UserEntity> users;

    private InMemoryUserRepository(List<UserEntity> users) {
      this.users = users;
    }

    @Override
    public List<UserEntity> findAll() {
      return users;
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
      return users.stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public Optional<UserEntity> findByPhone(String phone) {
      return users.stream().filter(user -> user.phone().equals(phone)).findFirst();
    }

    @Override
    public void updatePresence(String userId, String presence) {
    }
  }

  private static final class InMemoryFriendRepository implements FriendRepository {

    private final List<List<String>> friendships;

    private InMemoryFriendRepository(List<List<String>> friendships) {
      this.friendships = friendships;
    }

    @Override
    public boolean areFriends(String firstUserId, String secondUserId) {
      return friendships.stream().anyMatch(pair -> pair.contains(firstUserId) && pair.contains(secondUserId));
    }

    @Override
    public List<String> findFriendIdsForUser(String userId) {
      return friendships.stream()
          .filter(pair -> pair.contains(userId))
          .map(pair -> pair.stream().filter(friendId -> !friendId.equals(userId)).findFirst().orElseThrow())
          .toList();
    }

    @Override
    public void addFriendship(String firstUserId, String secondUserId) {
    }

    @Override
    public void upsertFriendRequest(String fromUserId, String toUserId, String status) {
    }

    @Override
    public List<FriendRequestEntity> findRequestsForUser(String userId) {
      return List.of();
    }

    @Override
    public Optional<FriendRequestEntity> findRequestForRecipient(String requestId, String recipientUserId) {
      return Optional.empty();
    }

    @Override
    public void updateRequestStatus(String requestId, String status) {
    }
  }
}
