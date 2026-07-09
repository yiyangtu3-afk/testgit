package com.campuslink.controller;

import com.campuslink.dto.DemoDtos.CommentView;
import com.campuslink.dto.DemoDtos.PersonalPostDeleteResponse;
import com.campuslink.dto.DemoDtos.PostView;
import com.campuslink.dto.DemoDtos.PublishCommentRequest;
import com.campuslink.dto.DemoDtos.PublishPostRequest;
import com.campuslink.dto.DemoDtos.UpdatePersonalPostRequest;
import com.campuslink.service.AuthTokenService;
import com.campuslink.service.FeedService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

  private final FeedService feedService;
  private final AuthTokenService authTokenService;

  public FeedController(FeedService feedService, AuthTokenService authTokenService) {
    this.feedService = feedService;
    this.authTokenService = authTokenService;
  }

  @GetMapping
  public List<PostView> feed() {
    return feedService.feed();
  }

  @PostMapping
  public PostView publish(
      @Valid @RequestBody PublishPostRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.publish(authTokenService.requireUserId(authorization), request.body(), request.visibility());
  }

  @GetMapping("/personal-posts")
  public List<PostView> personalPosts(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.personalPosts(authTokenService.requireUserId(authorization));
  }

  @PatchMapping("/personal-posts/{postId}")
  public PostView updatePersonalPost(
      @PathVariable Long postId,
      @Valid @RequestBody UpdatePersonalPostRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.updatePersonalPost(authTokenService.requireUserId(authorization), postId, request.body());
  }

  @DeleteMapping("/personal-posts/{postId}")
  public PersonalPostDeleteResponse deletePersonalPost(
      @PathVariable Long postId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.deletePersonalPost(authTokenService.requireUserId(authorization), postId);
  }

  @PostMapping("/{postId}/likes")
  public PostView likePost(
      @PathVariable Long postId,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.likePost(postId, authTokenService.requireUserId(authorization));
  }

  @GetMapping("/{postId}/comments")
  public List<CommentView> comments(@PathVariable Long postId) {
    return feedService.comments(postId);
  }

  @PostMapping("/{postId}/comments")
  public CommentView publishComment(
      @PathVariable Long postId,
      @Valid @RequestBody PublishCommentRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return feedService.publishComment(postId, authTokenService.requireUserId(authorization), request.body());
  }
}
