package com.campuslink.service;

import com.campuslink.dto.DemoDtos.CommentView;
import com.campuslink.dto.DemoDtos.PersonalPostDeleteResponse;
import com.campuslink.dto.DemoDtos.PostView;
import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import com.campuslink.entity.PostLikeResult;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.ModerationRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

  private static final Set<String> SUPPORTED_VISIBILITIES = Set.of("全校可见", "好友可见", "仅老师可见");

  private final FeedRepository feedRepository;
  private final ModerationRepository moderationRepository;
  private final AuditService auditService;
  private final UserService userService;
  private final SocialNotificationService notifications;

  public FeedService(
      FeedRepository feedRepository,
      ModerationRepository moderationRepository,
      AuditService auditService,
      UserService userService,
      SocialNotificationService notifications) {
    this.feedRepository = feedRepository;
    this.moderationRepository = moderationRepository;
    this.auditService = auditService;
    this.userService = userService;
    this.notifications = notifications;
  }

  public List<PostView> feed(String currentUserId) {
    return feedRepository.findPostsVisibleTo(currentUserId).stream()
        .map(DemoMapper::toPostView)
        .toList();
  }

  @Transactional
  public PostView publish(String currentUserId, String body, String visibility) {
    String normalizedVisibility = requireSupportedVisibility(visibility);
    PostEntity post = feedRepository.savePost(currentUserId, body, normalizedVisibility);
    String reason = "校园动态发布审核";
    moderationRepository.create("post", post.id(), null, reason);
    auditService.addAudit("动态", userService.userName(currentUserId) + "发布一条" + normalizedVisibility + "动态");
    return DemoMapper.toPostView(withModerationReason(post, reason));
  }

  public List<PostView> personalPosts(String currentUserId) {
    return feedRepository.findPostsByAuthor(currentUserId).stream()
        .map(DemoMapper::toPostView)
        .toList();
  }

  @Transactional
  public PostView updatePersonalPost(String currentUserId, Long postId, String body) {
    PostEntity updated = feedRepository.updatePostOwnedBy(currentUserId, postId, body)
        .orElseThrow(() -> new IllegalArgumentException("只能编辑自己的动态"));
    String reason = "个人动态编辑审核";
    moderationRepository.create("post", postId, null, reason);
    auditService.addAudit("动态", userService.userName(currentUserId) + "编辑自己的动态");
    return DemoMapper.toPostView(withModerationReason(updated, reason));
  }

  public PersonalPostDeleteResponse deletePersonalPost(String currentUserId, Long postId) {
    boolean deleted = feedRepository.deletePostOwnedBy(currentUserId, postId);
    if (!deleted) {
      throw new IllegalArgumentException("只能删除自己的动态");
    }
    auditService.addAudit("动态", userService.userName(currentUserId) + "删除自己的动态");
    return new PersonalPostDeleteResponse(true);
  }

  @Transactional
  public PostView likePost(Long postId, String currentUserId) {
    PostLikeResult result = feedRepository.toggleLike(postId, currentUserId);
    String action = result.liked() ? "点赞" : "取消点赞";
    String actorName = userService.userName(currentUserId);
    auditService.addAudit("动态", actorName + action + result.post().author() + "的动态");
    if (result.liked()) {
      String authorId = feedRepository.findPostAuthorId(postId)
          .orElseThrow(() -> new IllegalArgumentException("动态不存在"));
      if (!authorId.equals(currentUserId)) {
        notifications.recordPostLiked(authorId, currentUserId, actorName, postId);
      }
    }
    return DemoMapper.toPostView(result.post());
  }

  public List<CommentView> comments(Long postId) {
    return feedRepository.findVisibleComments(postId).stream()
        .map(DemoMapper::toCommentView)
        .toList();
  }

  @Transactional
  public CommentView publishComment(Long postId, String currentUserId, String body) {
    CommentEntity comment = feedRepository.saveComment(postId, currentUserId, body);
    moderationRepository.create("comment", comment.id(), postId, "动态评论发布审核");
    PostEntity post = feedRepository.findPost(postId)
        .orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    auditService.addAudit("动态", userService.userName(currentUserId) + "评论" + post.author() + "的动态");
    return DemoMapper.toCommentView(comment);
  }

  private PostEntity withModerationReason(PostEntity post, String reason) {
    return new PostEntity(
        post.id(),
        post.author(),
        post.body(),
        post.visibility(),
        post.likes(),
        post.comments(),
        post.moderationStatus(),
        reason,
        post.likedByCurrentUser());
  }

  private String requireSupportedVisibility(String visibility) {
    if (!SUPPORTED_VISIBILITIES.contains(visibility)) {
      throw new IllegalArgumentException("动态可见范围不支持");
    }
    return visibility;
  }
}
