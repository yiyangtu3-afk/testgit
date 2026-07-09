package com.campuslink.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslink.dto.DemoDtos.CommentView;
import com.campuslink.dto.DemoDtos.PersonalPostDeleteResponse;
import com.campuslink.dto.DemoDtos.PostView;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FeedService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

  @Mock
  private FeedService feedService;

  @Mock
  private AuthTokenService authTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new FeedController(feedService, authTokenService)).build();
  }

  @Test
  void feedReturnsPosts() throws Exception {
    when(feedService.feed()).thenReturn(List.of(
        new PostView(1L, "林一", "动态", "全校可见", 3, 1, "approved", "内容符合校园动态规范")));

    mockMvc.perform(get("/api/feed"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].body").value("动态"));
  }

  @Test
  void publishCreatesPost() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(feedService.publish("u-1001", "新动态", "好友可见")).thenReturn(
        new PostView(2L, "林一", "新动态", "好友可见", 0, 0, "pending", "校园动态发布审核"));

    mockMvc.perform(post("/api/feed")
            .contentType("application/json")
            .header("Authorization", "Bearer test-token")
            .content("{\"body\":\"新动态\",\"visibility\":\"好友可见\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.moderationStatus").value("pending"))
        .andExpect(jsonPath("$.moderationReason").value("校园动态发布审核"));
  }

  @Test
  void personalPostsReturnsCurrentUsersPosts() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(feedService.personalPosts("u-1001")).thenReturn(List.of(
        new PostView(2L, "林一", "自己的动态", "全校可见", 0, 0, "approved", "内容符合校园动态规范")));

    mockMvc.perform(get("/api/feed/personal-posts")
        .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].body").value("自己的动态"))
        .andExpect(jsonPath("$[0].moderationReason").value("内容符合校园动态规范"));
  }

  @Test
  void updatePersonalPostReturnsUpdatedPost() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(feedService.updatePersonalPost("u-1001", 2L, "更新后的动态")).thenReturn(
        new PostView(2L, "林一", "更新后的动态", "全校可见", 0, 0, "pending", "校园动态发布审核"));

    mockMvc.perform(patch("/api/feed/personal-posts/2")
            .contentType("application/json")
            .header("Authorization", "Bearer test-token")
            .content("{\"body\":\"更新后的动态\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.body").value("更新后的动态"))
        .andExpect(jsonPath("$.moderationStatus").value("pending"));
  }

  @Test
  void deletePersonalPostReturnsDeleted() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(feedService.deletePersonalPost("u-1001", 2L)).thenReturn(
        new PersonalPostDeleteResponse(true));

    mockMvc.perform(delete("/api/feed/personal-posts/2")
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(true));
  }

  @Test
  void publishCommentCreatesComment() throws Exception {
    when(authTokenService.requireUserId("Bearer test-token")).thenReturn("u-1001");
    when(feedService.publishComment(eq(1L), eq("u-1001"), eq("新评论"))).thenReturn(
        new CommentView(3L, "林一", "新评论", "09:30", "pending"));

    mockMvc.perform(post("/api/feed/1/comments")
            .contentType("application/json")
            .header("Authorization", "Bearer test-token")
            .content("{\"body\":\"新评论\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.body").value("新评论"));
  }
}
