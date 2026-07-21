package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campuslink.dto.DemoDtos.CommentView;
import com.campuslink.dto.DemoDtos.PostView;
import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import com.campuslink.entity.PostLikeResult;
import com.campuslink.repository.FeedRepository;
import com.campuslink.repository.ModerationRepository;
import com.campuslink.support.InMemorySocialNotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FeedServiceTest {

  private final InMemoryFeedRepository feedRepository = new InMemoryFeedRepository();
  private final InMemoryModerationRepository moderationRepository = new InMemoryModerationRepository();
  private final SocialNotificationService notifications = new SocialNotificationService(
      new InMemorySocialNotificationRepository());
  private final FeedService feedService = new FeedService(
      feedRepository,
      moderationRepository,
      new AuditService(new TestAuditRepository()),
      new UserService(new InMemoryUserRepository()),
      notifications);

  @Test
  void feedReturnsVisiblePostsFromRepository() {
    feedRepository.posts.add(new PostEntity(2L, "周同学", "待审动态", "全校可见", 0, 0, "pending", "校园动态发布审核", false));

    List<PostView> posts = feedService.feed("u-1001");

    assertThat(posts).extracting(PostView::body).containsExactly("数据库动态");
    assertThat(feedRepository.lastViewerId).isEqualTo("u-1001");
  }

  @Test
  void publishPersistsPendingPost() {
    PostView post = feedService.publish("u-1001", "新动态", "好友可见");

    assertThat(post.body()).isEqualTo("新动态");
    assertThat(post.visibility()).isEqualTo("好友可见");
    assertThat(post.moderationStatus()).isEqualTo("pending");
    assertThat(post.moderationReason()).isEqualTo("校园动态发布审核");
    assertThat(moderationRepository.findPending()).hasSize(1);
  }

  @Test
  void publishRejectsUnsupportedVisibility() {
    assertThatThrownBy(() -> feedService.publish("u-1001", "新动态", "陌生人可见"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("动态可见范围不支持");
  }

  @Test
  void publishCommentPersistsPendingCommentWithoutPublicCount() {
    CommentView comment = feedService.publishComment(1L, "u-1001", "新评论");

    assertThat(comment.body()).isEqualTo("新评论");
    assertThat(comment.moderationStatus()).isEqualTo("pending");
    assertThat(feedRepository.findPost(1L)).get().extracting(PostEntity::comments).isEqualTo(0);
    assertThat(notifications.summary("u-1001").items()).isEmpty();
  }

  @Test
  void commentingOnAnotherUsersPostNotifiesTheAuthor() {
    CommentView comment = feedService.publishComment(1L, "u-1002", "这条动态很有帮助");

    assertThat(notifications.summary("u-1001").items())
        .singleElement()
        .satisfies(notification -> {
          assertThat(notification.targetId()).isEqualTo(String.valueOf(comment.id()));
          assertThat(notification.type()).isEqualTo("social.post.commented");
          assertThat(notification.title()).isEqualTo("动态收到新评论");
          assertThat(notification.body()).contains("陈老师");
          assertThat(notification.body()).contains("这条动态很有帮助");
          assertThat(notification.read()).isFalse();
        });
  }

  @Test
  void currentUserCanToggleTheirLikeOnAndOff() {
    PostView liked = feedService.likePost(1L, "u-1001");
    PostView unliked = feedService.likePost(1L, "u-1001");

    assertThat(liked.likes()).isEqualTo(1);
    assertThat(liked.likedByCurrentUser()).isTrue();
    assertThat(unliked.likes()).isZero();
    assertThat(unliked.likedByCurrentUser()).isFalse();
  }

  @Test
  void likingAnotherUsersPostCreatesPersistentNotificationForTheAuthor() {
    feedService.likePost(1L, "u-1002");

    assertThat(notifications.summary("u-1001").items())
        .singleElement()
        .satisfies(notification -> {
          assertThat(notification.type()).isEqualTo("social.post.liked");
          assertThat(notification.title()).isEqualTo("动态收到新点赞");
          assertThat(notification.body()).contains("陈老师");
          assertThat(notification.read()).isFalse();
        });
  }

  @Test
  void likingOwnPostDoesNotCreateNotification() {
    feedService.likePost(1L, "u-1001");

    assertThat(notifications.summary("u-1001").items()).isEmpty();
  }

  @Test
  void personalPostsReturnOnlyCurrentUsersPosts() {
    feedRepository.posts.add(new PostEntity(2L, "陈老师", "老师动态", "全校可见", 0, 0, "approved", "内容符合校园动态规范", false));

    assertThat(feedService.personalPosts("u-1001"))
        .extracting(PostView::body)
        .containsExactly("数据库动态");
  }

  @Test
  void updatePersonalPostResetsModerationStatus() {
    PostView updated = feedService.updatePersonalPost("u-1001", 1L, "改过的动态");

    assertThat(updated.body()).isEqualTo("改过的动态");
    assertThat(updated.moderationStatus()).isEqualTo("pending");
    assertThat(moderationRepository.findPending()).hasSize(1);
  }

  @Test
  void deletePersonalPostRejectsOtherAuthorsPost() {
    feedRepository.posts.add(new PostEntity(2L, "陈老师", "老师动态", "全校可见", 0, 0, "approved", "内容符合校园动态规范", false));

    assertThatThrownBy(() -> feedService.deletePersonalPost("u-1001", 2L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("只能删除自己的动态");
  }

  private static final class InMemoryModerationRepository implements ModerationRepository {

    private final List<ModerationItemEntity> items = new ArrayList<>();

    @Override
    public List<ModerationItemEntity> findAll() {
      return items;
    }

    @Override
    public List<ModerationItemEntity> findPending() {
      return items.stream().filter(item -> "pending".equals(item.status())).toList();
    }

    @Override
    public long countPending() {
      return findPending().size();
    }

    @Override
    public ModerationItemEntity create(String type, Long targetId, Long postId, String reason) {
      ModerationItemEntity item = new ModerationItemEntity(
          "mod-" + (items.size() + 1),
          type,
          targetId,
          postId,
          type.equals("post") ? "动态：审核内容" : "评论：审核内容",
          "林一",
          "审核内容",
          "pending",
          reason,
          null,
          null,
          null,
          "2026-07-06 09:30",
          "09:30");
      items.add(0, item);
      return item;
    }

    @Override
    public Optional<ModerationItemEntity> findById(String itemId) {
      return items.stream().filter(item -> item.id().equals(itemId)).findFirst();
    }

    @Override
    public void completeReview(
        String itemId,
        String status,
        String reviewerName,
        java.time.LocalDateTime reviewedAt,
        String reviewComment) {
    }

    @Override
    public int deleteByIds(List<String> itemIds) {
      int before = items.size();
      items.removeIf(item -> itemIds.contains(item.id()));
      return before - items.size();
    }
  }

  private static final class InMemoryFeedRepository implements FeedRepository {

    private final List<PostEntity> posts = new ArrayList<>(List.of(
        new PostEntity(1L, "林一", "数据库动态", "全校可见", 0, 0, "approved", "内容符合校园动态规范", false)));
    private final List<CommentEntity> comments = new ArrayList<>();
    private final java.util.Set<String> likes = new java.util.HashSet<>();
    private String lastViewerId;

    @Override
    public int countPosts() {
      return posts.size();
    }

    @Override
    public List<PostEntity> findVisiblePosts() {
      return posts.stream().filter(post -> "approved".equals(post.moderationStatus())).toList();
    }

    @Override
    public List<PostEntity> findPostsVisibleTo(String viewerId) {
      lastViewerId = viewerId;
      return findVisiblePosts();
    }

    @Override
    public List<PostEntity> findPostsByAuthor(String authorId) {
      String author = "u-1001".equals(authorId) ? "林一" : "陈老师";
      return posts.stream().filter(post -> post.author().equals(author)).toList();
    }

    @Override
  public Optional<PostEntity> findPost(Long postId) {
    return posts.stream().filter(post -> post.id().equals(postId)).findFirst();
  }

  @Override
  public Optional<Long> findPostIdByCommentId(Long commentId) {
    return comments.stream().anyMatch(comment -> comment.id().equals(commentId))
        ? Optional.of(1L)
        : Optional.empty();
  }

    @Override
    public Optional<String> findPostAuthorId(Long postId) {
      return findPost(postId).map(post -> "林一".equals(post.author()) ? "u-1001" : "u-1002");
    }

    @Override
    public PostEntity savePost(String authorId, String body, String visibility) {
      PostEntity post = new PostEntity((long) posts.size() + 1, "林一", body, visibility, 0, 0, "pending", "校园动态发布审核", false);
      posts.add(0, post);
      return post;
    }

    @Override
    public Optional<PostEntity> updatePostOwnedBy(String authorId, Long postId, String body) {
      String author = "u-1001".equals(authorId) ? "林一" : "陈老师";
      Optional<PostEntity> existing = posts.stream()
          .filter(post -> post.id().equals(postId) && post.author().equals(author))
          .findFirst();
      existing.ifPresent(post -> {
        PostEntity updated = new PostEntity(
            post.id(), post.author(), body, post.visibility(), post.likes(), post.comments(), "pending", "个人动态编辑审核", post.likedByCurrentUser());
        posts.set(posts.indexOf(post), updated);
      });
      return existing.flatMap(post -> findPost(post.id()));
    }

    @Override
    public boolean deletePostOwnedBy(String authorId, Long postId) {
      String author = "u-1001".equals(authorId) ? "林一" : "陈老师";
      return posts.removeIf(post -> post.id().equals(postId) && post.author().equals(author));
    }

    @Override
    public PostLikeResult toggleLike(Long postId, String userId) {
      PostEntity post = findPost(postId).orElseThrow();
      String key = postId + ":" + userId;
      boolean liked = likes.add(key);
      if (!liked) {
        likes.remove(key);
      }
      PostEntity likedPost = new PostEntity(
          post.id(),
          post.author(),
          post.body(),
          post.visibility(),
          Math.max(0, post.likes() + (liked ? 1 : -1)),
          post.comments(),
          post.moderationStatus(),
          post.moderationReason(),
          liked);
      posts.set(posts.indexOf(post), likedPost);
      return new PostLikeResult(likedPost, liked);
    }

    @Override
    public List<CommentEntity> findVisibleComments(Long postId) {
      return comments.stream().filter(comment -> "approved".equals(comment.moderationStatus())).toList();
    }

    @Override
    public CommentEntity saveComment(Long postId, String authorId, String body) {
      String author = "u-1002".equals(authorId) ? "陈老师" : "林一";
      CommentEntity comment = new CommentEntity((long) comments.size() + 1, author, body, "09:30", "pending");
      comments.add(comment);
      return comment;
    }

    @Override
    public void updatePostModeration(Long postId, String status) {
    }

    @Override
    public void updateCommentModeration(Long postId, Long commentId, String status) {
    }
  }

  private static final class InMemoryUserRepository implements com.campuslink.repository.UserRepository {

    @Override
    public List<com.campuslink.entity.DemoEntities.UserEntity> findAll() {
      return List.of(
          new com.campuslink.entity.DemoEntities.UserEntity(
              "u-1001", "林一", "学生账号", "13800000001", "online"),
          new com.campuslink.entity.DemoEntities.UserEntity(
              "u-1002", "陈老师", "教师账号", "13800000002", "online"));
    }

    @Override
    public Optional<com.campuslink.entity.DemoEntities.UserEntity> findById(String userId) {
      return findAll().stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public Optional<com.campuslink.entity.DemoEntities.UserEntity> findByPhone(String phone) {
      return findAll().stream().filter(user -> user.phone().equals(phone)).findFirst();
    }

    @Override
    public void updatePresence(String userId, String presence) {
    }
  }
}
