package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import com.campuslink.entity.PostLikeResult;
import java.util.List;
import java.util.Optional;

public interface FeedRepository {

  List<PostEntity> findVisiblePosts();

  List<PostEntity> findPostsVisibleTo(String viewerId);

  List<PostEntity> findPostsByAuthor(String authorId);

  Optional<PostEntity> findPost(Long postId);

  Optional<Long> findPostIdByCommentId(Long commentId);

  Optional<String> findPostAuthorId(Long postId);

  PostEntity savePost(String authorId, String body, String visibility);

  Optional<PostEntity> updatePostOwnedBy(String authorId, Long postId, String body);

  boolean deletePostOwnedBy(String authorId, Long postId);

  PostLikeResult toggleLike(Long postId, String userId);

  List<CommentEntity> findVisibleComments(Long postId);

  CommentEntity saveComment(Long postId, String authorId, String body);

  void updatePostModeration(Long postId, String status);

  void updateCommentModeration(Long postId, Long commentId, String status);
}
