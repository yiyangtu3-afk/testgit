package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.ConversationPageView;
import com.campuslink.dto.DemoDtos.ConversationPreviewsView;
import com.campuslink.dto.DemoDtos.UnreadCountsView;
import com.campuslink.dto.DemoDtos.PresenceRequest;
import com.campuslink.dto.DemoDtos.PresenceResponse;
import com.campuslink.dto.DemoDtos.SendMessageRequest;
import com.campuslink.entity.DemoEntities.AttachmentContentEntity;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

  private final ChatService chatService;
  private final AuthTokenService authTokenService;

  public ChatController(ChatService chatService, AuthTokenService authTokenService) {
    this.chatService = chatService;
    this.authTokenService = authTokenService;
  }

  @GetMapping("/conversations/{peerId}/messages")
  public ConversationPageView messages(
      @PathVariable String peerId,
      @RequestParam(required = false) Long beforeId,
      @RequestParam(defaultValue = "30") int limit,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return chatService.messages(peerId, authTokenService.requireUserId(authorization), beforeId, limit);
  }

  @GetMapping("/conversations/unread-counts")
  public UnreadCountsView unreadCounts(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return new UnreadCountsView(chatService.unreadCounts(authTokenService.requireUserId(authorization)));
  }

  @GetMapping("/conversations/previews")
  public ConversationPreviewsView conversationPreviews(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return new ConversationPreviewsView(chatService.conversationPreviews(authTokenService.requireUserId(authorization)));
  }

  @PostMapping("/conversations/{peerId}/messages")
  public MessageView sendMessage(
      @PathVariable String peerId,
      @Valid @RequestBody SendMessageRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return chatService.sendMessage(peerId, authTokenService.requireUserId(authorization), request);
  }

  @GetMapping("/conversations/{peerId}/attachments/{attachmentId}")
  public ResponseEntity<byte[]> attachment(
      @PathVariable String peerId,
      @PathVariable String attachmentId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    AttachmentContentEntity attachment = chatService.attachmentContent(
        peerId,
        authTokenService.requireUserId(authorization),
        attachmentId);
    return ResponseEntity.ok()
        .contentType(imageMediaType(attachment.type()))
        .contentLength(attachment.content().length)
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(attachment.content());
  }

  @PostMapping("/conversations/{peerId}/messages/{messageId}/withdraw")
  public MessageView withdrawMessage(
      @PathVariable String peerId,
      @PathVariable Long messageId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return chatService.withdrawMessage(peerId, messageId, authTokenService.requireUserId(authorization));
  }

  @PostMapping("/presence")
  public PresenceResponse updatePresence(
      @Valid @RequestBody PresenceRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return chatService.updatePresence(authTokenService.requireUserId(authorization), request.presence());
  }

  private static MediaType imageMediaType(String type) {
    return switch (type) {
      case "image/jpeg" -> MediaType.IMAGE_JPEG;
      case "image/png" -> MediaType.IMAGE_PNG;
      case "image/gif" -> MediaType.IMAGE_GIF;
      case "image/webp" -> MediaType.parseMediaType("image/webp");
      default -> throw new IllegalArgumentException("图片类型不支持");
    };
  }
}
