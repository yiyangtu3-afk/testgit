package com.campuslink.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.ActivityNotificationDtos.NotificationView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import com.campuslink.service.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

class ChatWebSocketHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final InMemoryAuthSessionRepository authSessions = new InMemoryAuthSessionRepository();
  private final AuthTokenService authTokenService = new AuthTokenService(
      authSessions,
      new InMemoryUserRepository(List.of(
          new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
          new UserEntity("u-2002", "周同学", "学生账号", "13800000003", "online"))));
  private final ChatWebSocketHandler handler = new ChatWebSocketHandler(authTokenService, objectMapper);

  @Test
  void heartbeatPingReturnsPong() throws Exception {
    TestWebSocketSession session = connectedSession("demo.student", "u-1001");

    handler.handleMessage(session, new TextMessage("""
        {"type":"heartbeat.ping"}
        """));

    assertThat(session.sentTypes(objectMapper)).containsExactly("heartbeat.pong");
  }

  @Test
  void publishMessageSendsEventToSenderAndRecipientSessions() throws Exception {
    TestWebSocketSession sender = connectedSession("demo.sender", "u-1001");
    TestWebSocketSession recipient = connectedSession("demo.recipient", "u-2002");

    handler.publishMessage(
        "u-2002",
        new MessageView(42L, "u-1001", "你好，周同学", "13:30", false, List.of()));

    JsonNode senderEvent = sender.sentJson(objectMapper).getFirst();
    JsonNode recipientEvent = recipient.sentJson(objectMapper).getFirst();
    assertThat(senderEvent.path("type").asText()).isEqualTo("message.created");
    assertThat(senderEvent.path("peerId").asText()).isEqualTo("u-2002");
    assertThat(recipientEvent.path("type").asText()).isEqualTo("message.created");
    assertThat(recipientEvent.path("peerId").asText()).isEqualTo("u-1001");
    assertThat(recipientEvent.path("message").path("text").asText()).isEqualTo("你好，周同学");
  }

  @Test
  void publishMessageWithdrawnSendsEventToSenderAndRecipientSessions() throws Exception {
    TestWebSocketSession sender = connectedSession("demo.sender", "u-1001");
    TestWebSocketSession recipient = connectedSession("demo.recipient", "u-2002");

    handler.publishMessageWithdrawn(
        "u-2002",
        new MessageView(42L, "u-1001", "撤回内容", "13:30", true, List.of()));

    JsonNode senderEvent = sender.sentJson(objectMapper).getFirst();
    JsonNode recipientEvent = recipient.sentJson(objectMapper).getFirst();
    assertThat(senderEvent.path("type").asText()).isEqualTo("message.withdrawn");
    assertThat(senderEvent.path("peerId").asText()).isEqualTo("u-2002");
    assertThat(recipientEvent.path("type").asText()).isEqualTo("message.withdrawn");
    assertThat(recipientEvent.path("peerId").asText()).isEqualTo("u-1001");
    assertThat(recipientEvent.path("message").path("deleted").asBoolean()).isTrue();
  }

  @Test
  void publishActivityNotificationSendsEventOnlyToRecipient() throws Exception {
    TestWebSocketSession recipient = connectedSession("demo.recipient", "u-2002");
    TestWebSocketSession other = connectedSession("demo.student", "u-1001");
    NotificationView notification = new NotificationView(
        "notification-1",
        "activity-1",
        "activity.registration.promoted",
        "候补已递补",
        "你已获得活动名额。",
        false,
        LocalDateTime.of(2026, 7, 12, 12, 0));

    handler.publishActivityNotification("u-2002", notification);

    JsonNode recipientEvent = recipient.sentJson(objectMapper).getFirst();
    assertThat(recipientEvent.path("type").asText()).isEqualTo("activity.notification.created");
    assertThat(recipientEvent.path("notification").path("id").asText())
        .isEqualTo("notification-1");
    assertThat(other.sentMessages).isEmpty();
  }

  @Test
  void closedSessionIsRemovedFromBroadcastTargets() throws Exception {
    TestWebSocketSession session = connectedSession("demo.student", "u-1001");

    handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    handler.publishMessage(
        "u-2002",
        new MessageView(43L, "u-1001", "不会再发送", "13:31", false, List.of()));

    assertThat(session.sentMessages).isEmpty();
  }

  @Test
  void missingTokenClosesSession() throws Exception {
    TestWebSocketSession session = new TestWebSocketSession("/ws/chat");

    handler.afterConnectionEstablished(session);

    assertThat(session.open).isFalse();
    assertThat(session.closeStatus).isEqualTo(CloseStatus.NOT_ACCEPTABLE.withReason("请先登录"));
  }

  private TestWebSocketSession connectedSession(String token, String userId) throws Exception {
    authSessions.save(token, userId);
    TestWebSocketSession session = new TestWebSocketSession("/ws/chat?token=" + token);
    handler.afterConnectionEstablished(session);
    assertThat(session.open).isTrue();
    return session;
  }

  private static final class TestWebSocketSession implements WebSocketSession {

    private final URI uri;
    private final Map<String, Object> attributes = new HashMap<>();
    private final List<String> sentMessages = new ArrayList<>();
    private boolean open = true;
    private CloseStatus closeStatus;

    private TestWebSocketSession(String uri) {
      this.uri = URI.create(uri);
    }

    @Override
    public String getId() {
      return "test-session";
    }

    @Override
    public URI getUri() {
      return uri;
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
      return HttpHeaders.EMPTY;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return attributes;
    }

    @Override
    public Principal getPrincipal() {
      return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return null;
    }

    @Override
    public String getAcceptedProtocol() {
      return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getTextMessageSizeLimit() {
      return 8192;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getBinaryMessageSizeLimit() {
      return 8192;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
      return List.of();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
      sentMessages.add(message.getPayload().toString());
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() throws IOException {
      close(CloseStatus.NORMAL);
    }

    @Override
    public void close(CloseStatus status) throws IOException {
      open = false;
      closeStatus = status;
    }

    private List<JsonNode> sentJson(ObjectMapper objectMapper) {
      return sentMessages.stream()
          .map(payload -> readJson(objectMapper, payload))
          .toList();
    }

    private List<String> sentTypes(ObjectMapper objectMapper) {
      return sentJson(objectMapper).stream()
          .map(node -> node.path("type").asText())
          .toList();
    }

    private JsonNode readJson(ObjectMapper objectMapper, String payload) {
      try {
        return objectMapper.readTree(payload);
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }
    }
  }

  private static final class InMemoryAuthSessionRepository implements AuthSessionRepository {

    private final Map<String, String> tokenUsers = new HashMap<>();

    @Override
    public void save(String token, String userId) {
      tokenUsers.put(token, userId);
    }

    @Override
    public Optional<String> findUserIdByToken(String token) {
      return Optional.ofNullable(tokenUsers.get(token));
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
