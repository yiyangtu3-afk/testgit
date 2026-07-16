package com.campuslink.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.CampusLinkApplication;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FeedService;
import com.campuslink.service.FriendService;
import com.campuslink.service.SocialNotificationService;
import com.campuslink.support.MySqlTestcontainersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
}
