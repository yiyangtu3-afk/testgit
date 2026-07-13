package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import com.campuslink.entity.PostLikeResult;
import com.campuslink.mapper.FeedMapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisFeedRepository implements FeedRepository {

  private final FeedMapper feedMapper;
  private final AtomicLong postIds = new AtomicLong(System.currentTimeMillis() * 1000);
  private final AtomicLong commentIds = new AtomicLong(System.currentTimeMillis() * 1000);

  public MyBatisFeedRepository(FeedMapper feedMapper) {
    this.feedMapper = feedMapper;
  }

  @Override
  public List<PostEntity> findVisiblePosts() {
    return feedMapper.findVisiblePosts();
  }

  @Override
  public List<PostEntity> findPostsVisibleTo(String viewerId) {
    return feedMapper.findPostsVisibleTo(viewerId);
  }

  @Override
  public List<PostEntity> findPostsByAuthor(String authorId) {
    return feedMapper.findPostsByAuthor(authorId);
  }

  @Override
  public Optional<PostEntity> findPost(Long postId) {
    return Optional.ofNullable(feedMapper.findPost(String.valueOf(postId)));
  }

  @Override
  public Optional<String> findPostAuthorId(Long postId) {
    return Optional.ofNullable(feedMapper.findPostAuthorId(String.valueOf(postId)));
  }

  @Override
  @Transactional
  public PostEntity savePost(String authorId, String body, String visibility) {
    long postId = postIds.incrementAndGet();
    feedMapper.insertPost(String.valueOf(postId), authorId, body, visibility);
    return findPost(postId).orElseThrow();
  }

  @Override
  @Transactional
  public Optional<PostEntity> updatePostOwnedBy(String authorId, Long postId, String body) {
    int updated = feedMapper.updatePostOwnedBy(authorId, String.valueOf(postId), body);
    if (updated == 0) {
      return Optional.empty();
    }
    return findPost(postId);
  }

  @Override
  @Transactional
  public boolean deletePostOwnedBy(String authorId, Long postId) {
    String postIdText = String.valueOf(postId);
    if (feedMapper.findPostByAuthor(authorId, postIdText) == null) {
      return false;
    }
    feedMapper.deleteModerationItemsForPostComments(postIdText);
    feedMapper.deleteModerationItemForPost(postIdText);
    feedMapper.deleteCommentsForPost(postIdText);
    feedMapper.deleteLikesForPost(postIdText);
    return feedMapper.deletePostOwnedBy(authorId, postIdText) > 0;
  }

  @Override
  @Transactional
  public PostLikeResult toggleLike(Long postId, String userId) {
    String postIdText = String.valueOf(postId);
    if (feedMapper.lockPost(postIdText) == null) {
      throw new IllegalArgumentException("动态不存在");
    }
    boolean liked;
    if (feedMapper.deleteLike(postIdText, userId) > 0) {
      feedMapper.adjustLikes(postIdText, -1);
      liked = false;
    } else {
      feedMapper.insertLike(postIdText, userId);
      feedMapper.adjustLikes(postIdText, 1);
      liked = true;
    }
    PostEntity post = findPost(postId)
        .orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    return new PostLikeResult(new PostEntity(
        post.id(), post.author(), post.body(), post.visibility(), post.likes(), post.comments(),
        post.moderationStatus(), post.moderationReason(), liked), liked);
  }

  @Override
  public List<CommentEntity> findVisibleComments(Long postId) {
    return feedMapper.findVisibleComments(String.valueOf(postId));
  }

  @Override
  @Transactional
  public CommentEntity saveComment(Long postId, String authorId, String body) {
    if (findPost(postId).isEmpty()) {
      throw new IllegalArgumentException("动态不存在");
    }
    long commentId = commentIds.incrementAndGet();
    feedMapper.insertComment(String.valueOf(commentId), String.valueOf(postId), authorId, body);
    return Optional.ofNullable(feedMapper.findComment(String.valueOf(postId), String.valueOf(commentId)))
        .orElseThrow();
  }

  @Override
  public void updatePostModeration(Long postId, String status) {
    feedMapper.updatePostModeration(String.valueOf(postId), status);
  }

  @Override
  public void updateCommentModeration(Long postId, Long commentId, String status) {
    feedMapper.updateCommentModeration(String.valueOf(postId), String.valueOf(commentId), status);
  }
}
