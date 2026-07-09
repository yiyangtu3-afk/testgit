package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.DemoDtos.FriendRequestResponse;
import com.campuslink.dto.DemoDtos.FriendRequestView;
import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.ChatRepository;
import com.campuslink.repository.FriendRepository;
import com.campuslink.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FriendServiceTest {

  private final InMemoryFriendRepository friends = new InMemoryFriendRepository();
  private final InMemoryChatRepository chat = new InMemoryChatRepository();
  private final FriendService friendService = new FriendService(
      friends,
      chat,
      new UserService(new InMemoryUserRepository(List.of(
          new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
          new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online"),
          new UserEntity("u-2002", "周同学", "学生", "13800000003", "offline")))),
      new AuditService(new TestAuditRepository()));

  @Test
  void createFriendRequestStoresPendingRequest() {
    FriendRequestResponse response = friendService.createFriendRequest("u-1001", "u-2002");

    assertThat(response.status()).isEqualTo("pending");
    assertThat(friends.findRequestsForUser("u-2002"))
        .extracting(FriendRequestEntity::fromUserId)
        .containsExactly("u-1001");
  }

  @Test
  void friendsReturnsUsersFromPersistedFriendships() {
    friends.addFriendship("u-1001", "u-2001");

    List<UserView> results = friendService.friends("u-1001");

    assertThat(results).extracting(UserView::id).containsExactly("u-2001");
  }

  @Test
  void acceptFriendRequestMarksAcceptedAndCreatesFriendship() {
    friends.upsertFriendRequest("u-2002", "u-1001", "pending");
    String requestId = friends.findRequestsForUser("u-1001").getFirst().id();

    FriendRequestView result = friendService.acceptFriendRequest(requestId, "u-1001");

    assertThat(result.status()).isEqualTo("accepted");
    assertThat(friends.areFriends("u-1001", "u-2002")).isTrue();
    assertThat(chat.findMessages("u-2002", "u-1001")).hasSize(1);
  }

  private static final class InMemoryChatRepository implements ChatRepository {

    private final List<StoredMessage> messages = new ArrayList<>();

    @Override
    public List<MessageEntity> findMessages(String peerId, String currentUserId) {
      return messages.stream()
          .filter(message -> isConversationMessage(message, peerId, currentUserId))
          .map(StoredMessage::message)
          .toList();
    }

    @Override
    public MessageEntity saveMessage(
        String peerId,
        String fromUserId,
        String body,
        List<AttachmentEntity> attachments) {
      MessageEntity message = new MessageEntity((long) messages.size() + 1, fromUserId, body, "09:30", false, attachments);
      messages.add(new StoredMessage(peerId, message));
      return message;
    }

    @Override
    public Optional<MessageEntity> findMessage(String peerId, String currentUserId, Long messageId) {
      return messages.stream()
          .filter(message -> isConversationMessage(message, peerId, currentUserId))
          .filter(message -> message.message().id().equals(messageId))
          .map(StoredMessage::message)
          .findFirst();
    }

    @Override
    public void withdrawMessage(String peerId, String currentUserId, Long messageId) {
    }

    private boolean isConversationMessage(StoredMessage message, String peerId, String currentUserId) {
      return (message.message().from().equals(currentUserId) && message.peerId().equals(peerId))
          || (message.message().from().equals(peerId) && message.peerId().equals(currentUserId));
    }

    private record StoredMessage(String peerId, MessageEntity message) {
    }
  }

  private static final class InMemoryFriendRepository implements FriendRepository {

    private final List<FriendRequestEntity> requests = new ArrayList<>();
    private final Set<Set<String>> friendships = new HashSet<>();

    @Override
    public boolean areFriends(String firstUserId, String secondUserId) {
      return friendships.contains(Set.of(firstUserId, secondUserId));
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
      friendships.add(Set.of(firstUserId, secondUserId));
    }

    @Override
    public void upsertFriendRequest(String fromUserId, String toUserId, String status) {
      for (int index = 0; index < requests.size(); index++) {
        FriendRequestEntity request = requests.get(index);
        if (request.fromUserId().equals(fromUserId) && request.toUserId().equals(toUserId)) {
          requests.set(index, new FriendRequestEntity(request.id(), fromUserId, toUserId, status));
          return;
        }
      }
      requests.add(new FriendRequestEntity("fr-" + (requests.size() + 1), fromUserId, toUserId, status));
    }

    @Override
    public List<FriendRequestEntity> findRequestsForUser(String userId) {
      return requests.stream()
          .filter(request -> request.fromUserId().equals(userId) || request.toUserId().equals(userId))
          .toList();
    }

    @Override
    public Optional<FriendRequestEntity> findRequestForRecipient(String requestId, String recipientUserId) {
      return requests.stream()
          .filter(request -> request.id().equals(requestId) && request.toUserId().equals(recipientUserId))
          .findFirst();
    }

    @Override
    public void updateRequestStatus(String requestId, String status) {
      for (int index = 0; index < requests.size(); index++) {
        FriendRequestEntity request = requests.get(index);
        if (request.id().equals(requestId)) {
          requests.set(index, new FriendRequestEntity(request.id(), request.fromUserId(), request.toUserId(), status));
          return;
        }
      }
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
}
