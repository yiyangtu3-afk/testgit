package com.campuslink.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.AttachmentView;
import com.campuslink.dto.DemoDtos.MessageView;
import com.campuslink.dto.DemoDtos.PresenceResponse;
import com.campuslink.dto.DemoDtos.SendMessageRequest;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private ChatService chatService;

  @Mock
  private AuthTokenService authTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, authTokenService)).build();
  }

  @Test
  void messagesReturnsConversationByPeerId() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(chatService.messages("u-2001", "u-1001")).thenReturn(List.of(
        new MessageView(1L, "u-2001", "你好", "09:30", false, List.of())));

    mockMvc.perform(get("/api/conversations/u-2001/messages")
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].from").value("u-2001"))
        .andExpect(jsonPath("$[0].text").value("你好"));
  }

  @Test
  void sendMessagePassesTextAndAttachmentsToService() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(chatService.sendMessage(eq("u-2001"), eq("u-1001"), org.mockito.ArgumentMatchers.any(SendMessageRequest.class)))
        .thenReturn(new MessageView(
            2L,
            "u-1001",
            "带附件消息",
            "09:31",
            false,
            List.of(new AttachmentView("att-1", "demo.txt", 32, "text/plain", "document"))));

    mockMvc.perform(post("/api/conversations/u-2001/messages")
            .contentType("application/json")
            .header("Authorization", "Bearer test-token")
            .content("""
                {
                  "text": "带附件消息",
                  "attachments": [
                    {"id":"att-1","name":"demo.txt","size":32,"type":"text/plain","kind":"document"}
                  ]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.attachments[0].name").value("demo.txt"));

    verify(chatService).sendMessage(eq("u-2001"), eq("u-1001"), org.mockito.ArgumentMatchers.any(SendMessageRequest.class));
  }

  @Test
  void updatePresenceReturnsSelectedPresence() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(chatService.updatePresence("u-1001", "invisible")).thenReturn(new PresenceResponse("invisible"));

    mockMvc.perform(post("/api/presence")
            .contentType("application/json")
            .header("Authorization", "Bearer test-token")
            .content(objectMapper.writeValueAsString(java.util.Map.of("presence", "invisible"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presence").value("invisible"));
  }
}
