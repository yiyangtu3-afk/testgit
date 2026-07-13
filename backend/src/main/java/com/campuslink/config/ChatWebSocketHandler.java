package com.campuslink.config;

import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.ActivityNotificationDtos.NotificationView;
import com.campuslink.service.ActivityNotificationRealtimePublisher;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.ChatRealtimeNotifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler
    implements ChatRealtimeNotifier, ActivityNotificationRealtimePublisher {

  private static final String USER_ID_ATTRIBUTE = "userId";

  private final AuthTokenService authTokenService;
  private final ObjectMapper objectMapper;
  private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

  public ChatWebSocketHandler(AuthTokenService authTokenService, ObjectMapper objectMapper) {
    this.authTokenService = authTokenService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    Optional<String> token = tokenFrom(session.getUri());
    if (token.isEmpty()) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("请先登录"));
      return;
    }
    String userId;
    try {
      userId = authTokenService.requireToken(token.get());
    } catch (SecurityException exception) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason(exception.getMessage()));
      return;
    }
    session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
    sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
    if (userId instanceof String currentUserId) {
      Set<WebSocketSession> sessions = sessionsByUser.get(currentUserId);
      if (sessions != null) {
        sessions.remove(session);
        if (sessions.isEmpty()) {
          sessionsByUser.remove(currentUserId);
        }
      }
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Optional<ClientRealtimeEvent> event = clientEvent(message.getPayload());
    if (event.isPresent() && "heartbeat.ping".equals(event.get().type())) {
      session.sendMessage(new TextMessage(toJson(new HeartbeatEvent("heartbeat.pong"))));
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    session.close(CloseStatus.SERVER_ERROR);
  }

  @Override
  public void publishMessage(String peerId, MessageView message) {
    publishConversationEvent("message.created", peerId, message);
  }

  @Override
  public void publishMessageWithdrawn(String peerId, MessageView message) {
    publishConversationEvent("message.withdrawn", peerId, message);
  }

  @Override
  public void publishActivityNotification(String recipientId, NotificationView notification) {
    send(recipientId, new ActivityNotificationRealtimeEvent(
        "activity.notification.created", notification));
  }

  private void publishConversationEvent(String type, String peerId, MessageView message) {
    send(message.from(), new ChatRealtimeEvent(type, peerId, message));
    if (!peerId.equals(message.from())) {
      send(peerId, new ChatRealtimeEvent(type, message.from(), message));
    }
  }

  private void send(String userId, Object event) {
    Set<WebSocketSession> sessions = sessionsByUser.getOrDefault(userId, Set.of());
    for (WebSocketSession session : sessions) {
      if (!session.isOpen()) {
        continue;
      }
      try {
        session.sendMessage(new TextMessage(toJson(event)));
      } catch (IOException exception) {
        try {
          session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
        }
      }
    }
  }

  private String toJson(Object event) throws JsonProcessingException {
    return objectMapper.writeValueAsString(event);
  }

  private Optional<ClientRealtimeEvent> clientEvent(String payload) {
    try {
      return Optional.of(objectMapper.readValue(payload, ClientRealtimeEvent.class));
    } catch (JsonProcessingException exception) {
      return Optional.empty();
    }
  }

  private Optional<String> tokenFrom(URI uri) {
    if (uri == null || uri.getQuery() == null) {
      return Optional.empty();
    }
    for (String part : uri.getQuery().split("&")) {
      int separator = part.indexOf('=');
      if (separator < 0) {
        continue;
      }
      String key = URLDecoder.decode(part.substring(0, separator), StandardCharsets.UTF_8);
      if ("token".equals(key)) {
        return Optional.of(URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8));
      }
    }
    return Optional.empty();
  }

  private record ChatRealtimeEvent(String type, String peerId, MessageView message) {
  }

  private record ActivityNotificationRealtimeEvent(
      String type, NotificationView notification) {
  }

  private record HeartbeatEvent(String type) {
  }

  private record ClientRealtimeEvent(String type) {
  }
}
