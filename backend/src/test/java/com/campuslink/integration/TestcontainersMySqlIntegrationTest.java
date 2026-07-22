package com.campuslink.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.CampusLinkApplication;
import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.repository.ChatRepository;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FeedService;
import com.campuslink.service.FriendService;
import com.campuslink.service.SocialNotificationService;
import com.campuslink.support.MySqlTestcontainersIntegrationTest;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Rollback
class TestcontainersMySqlIntegrationTest extends MySqlTestcontainersIntegrationTest {

  @Autowired private FeedService feed;
  @Autowired private FriendService friends;
  @Autowired private SocialNotificationService notifications;
  @Autowired private AuthTokenService authTokens;
  @Autowired private ChatRepository chatRepository;
  @Autowired private MockMvc mockMvc;

  @Test
  void executesMyBatisVisibilityRulesAgainstContainerizedMySql() {
    assertThat(feed.feed("u-1001")).extracting(post -> post.id())
        .contains(1L, 2L)
        .doesNotContain(3L);
    assertThat(feed.feed("u-2001")).extracting(post -> post.id())
        .contains(1L, 2L, 3L);
  }

  @Test
  void rollsBackFriendAcceptanceAndItsCrossTableNotificationsTogether() {
    friends.createFriendRequest("u-2004", "u-2002");
    var request = friends.friendRequests("u-2002").stream()
        .filter(item -> item.fromUserId().equals("u-2004"))
        .findFirst()
        .orElseThrow();

    friends.acceptFriendRequest(request.id(), "u-2002");

    assertThat(friends.friends("u-2002")).extracting(user -> user.id())
        .contains("u-2004");
    assertThat(notifications.summary("u-2004").items())
        .anySatisfy(notification -> {
          assertThat(notification.type()).isEqualTo("social.friend.accepted");
          assertThat(notification.targetId()).isEqualTo(request.id());
        });
  }

  @Test
  void enforcesJwtRolePermissionsAgainstContainerizedSessionData() throws Exception {
    String studentToken = authTokens.issueToken("u-1001");

    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + studentToken))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/metrics")
            .header("Authorization", "Bearer " + studentToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("需要管理员账号"));
  }

  @Test
  void persistsBrowserGeneratedAttachmentUuid() {
    String attachmentId = "d7d3b4f2-5c2d-4d2b-8f92-6f9c965ca5d0";

    var saved = chatRepository.saveMessage(
        "u-1001",
        "u-2001",
        "浏览器 UUID 附件",
        List.of(new AttachmentEntity(
            attachmentId, "campus-card.png", 2048, "image/png", "image")));

    assertThat(saved.attachments())
        .extracting(AttachmentEntity::id)
        .containsExactly(attachmentId);
  }

  @Test
  void storesAndServesAnAuthenticatedChatImage() throws Exception {
    String attachmentId = "6df2d20d-2b9c-4fbb-9e96-0e38ffaf4fc8";
    byte[] image = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/"
            + "r8eByAAAAABJRU5ErkJggg==");
    String token = authTokens.issueToken("u-1001");
    String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);

    mockMvc.perform(post("/api/conversations/u-2001/messages")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"text":"图片","attachments":[{
                  "id":"%s","name":"campus-card.png","size":%d,
                  "type":"image/png","kind":"image","dataUrl":"%s"
                }]}
                """.formatted(attachmentId, image.length, dataUrl)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attachments[0].id").value(attachmentId))
        .andExpect(jsonPath("$.attachments[0].hasContent").value(true));

    mockMvc.perform(get("/api/conversations/u-2001/attachments/{attachmentId}", attachmentId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(image));
  }
}
