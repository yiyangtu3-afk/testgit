package com.campuslink.mapper;

import com.campuslink.entity.DemoEntities.CommentEntity;
import com.campuslink.entity.DemoEntities.PostEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FeedMapper {

  @Select("""
      select cast(p.id as signed) as id,
             u.name as author,
             p.body,
             p.visibility,
             p.likes,
             (
               select count(*)
               from comments c
               where c.post_id = p.id and c.moderation_status = 'approved'
             ) as comments,
             p.moderation_status as moderationStatus,
             (
               select m.reason
               from moderation_items m
               where m.content_type = 'post'
                 and m.content_id = p.id
                 and m.status = p.moderation_status
               order by m.created_at desc, m.id desc
               limit 1
             ) as moderationReason,
             false as likedByCurrentUser
      from posts p
      join users u on u.id = p.author_id
      where p.moderation_status = 'approved'
        and p.id regexp '^[0-9]+$'
      order by p.created_at desc, cast(p.id as signed) desc
      """)
  List<PostEntity> findVisiblePosts();

  @Select("""
      select cast(p.id as signed) as id,
             author.name as author,
             p.body,
             p.visibility,
             p.likes,
             (
               select count(*)
               from comments c
               where c.post_id = p.id and c.moderation_status = 'approved'
             ) as comments,
             p.moderation_status as moderationStatus,
             (
               select m.reason
               from moderation_items m
               where m.content_type = 'post'
                 and m.content_id = p.id
                 and m.status = p.moderation_status
               order by m.created_at desc, m.id desc
               limit 1
             ) as moderationReason,
             exists (
               select 1 from post_likes pl
               where pl.post_id = p.id and pl.user_id = #{viewerId}
             ) as likedByCurrentUser
      from posts p
      join users author on author.id = p.author_id
      join users viewer on viewer.id = #{viewerId}
      where p.moderation_status = 'approved'
        and p.id regexp '^[0-9]+$'
        and (
          p.visibility = '全校可见'
          or p.author_id = #{viewerId}
          or (
            p.visibility = '好友可见'
            and exists (
              select 1
              from friendships f
              where (f.first_user_id = p.author_id and f.second_user_id = #{viewerId})
                 or (f.second_user_id = p.author_id and f.first_user_id = #{viewerId})
            )
          )
          or (p.visibility = '仅老师可见' and viewer.major like '%教师%')
        )
      order by p.created_at desc, cast(p.id as signed) desc
      """)
  List<PostEntity> findPostsVisibleTo(@Param("viewerId") String viewerId);

  @Select("""
      select cast(p.id as signed) as id,
             u.name as author,
             p.body,
             p.visibility,
             p.likes,
             (
               select count(*)
               from comments c
               where c.post_id = p.id and c.moderation_status = 'approved'
             ) as comments,
             p.moderation_status as moderationStatus,
             (
               select m.reason
               from moderation_items m
               where m.content_type = 'post'
                 and m.content_id = p.id
                 and m.status = p.moderation_status
               order by m.created_at desc, m.id desc
               limit 1
             ) as moderationReason,
             false as likedByCurrentUser
      from posts p
      join users u on u.id = p.author_id
      where p.author_id = #{authorId}
        and p.id regexp '^[0-9]+$'
      order by p.created_at desc, cast(p.id as signed) desc
      """)
  List<PostEntity> findPostsByAuthor(@Param("authorId") String authorId);

  @Select("""
      select cast(p.id as signed) as id,
             u.name as author,
             p.body,
             p.visibility,
             p.likes,
             (
               select count(*)
               from comments c
               where c.post_id = p.id and c.moderation_status = 'approved'
             ) as comments,
             p.moderation_status as moderationStatus,
             (
               select m.reason
               from moderation_items m
               where m.content_type = 'post'
                 and m.content_id = p.id
                 and m.status = p.moderation_status
               order by m.created_at desc, m.id desc
               limit 1
             ) as moderationReason,
             false as likedByCurrentUser
      from posts p
      join users u on u.id = p.author_id
      where p.id = #{postId}
      """)
  PostEntity findPost(@Param("postId") String postId);

  @Select("select author_id from posts where id = #{postId}")
  String findPostAuthorId(@Param("postId") String postId);

  @Select("""
      select cast(p.id as signed) as id,
             u.name as author,
             p.body,
             p.visibility,
             p.likes,
             (
               select count(*)
               from comments c
               where c.post_id = p.id and c.moderation_status = 'approved'
             ) as comments,
             p.moderation_status as moderationStatus,
             (
               select m.reason
               from moderation_items m
               where m.content_type = 'post'
                 and m.content_id = p.id
                 and m.status = p.moderation_status
               order by m.created_at desc, m.id desc
               limit 1
             ) as moderationReason,
             false as likedByCurrentUser
      from posts p
      join users u on u.id = p.author_id
      where p.author_id = #{authorId} and p.id = #{postId}
      """)
  PostEntity findPostByAuthor(@Param("authorId") String authorId, @Param("postId") String postId);

  @Insert("""
      insert into posts (id, author_id, body, visibility, likes, moderation_status)
      values (#{id}, #{authorId}, #{body}, #{visibility}, 0, 'pending')
      """)
  void insertPost(
      @Param("id") String id,
      @Param("authorId") String authorId,
      @Param("body") String body,
      @Param("visibility") String visibility);

  @Update("""
      update posts
      set body = #{body}, moderation_status = 'pending'
      where author_id = #{authorId} and id = #{postId}
      """)
  int updatePostOwnedBy(
      @Param("authorId") String authorId,
      @Param("postId") String postId,
      @Param("body") String body);

  @Delete("""
      delete from moderation_items
      where content_type = 'comment'
        and content_id in (select id from comments where post_id = #{postId})
      """)
  int deleteModerationItemsForPostComments(@Param("postId") String postId);

  @Delete("delete from moderation_items where content_type = 'post' and content_id = #{postId}")
  int deleteModerationItemForPost(@Param("postId") String postId);

  @Delete("delete from comments where post_id = #{postId}")
  int deleteCommentsForPost(@Param("postId") String postId);

  @Delete("delete from post_likes where post_id = #{postId}")
  int deleteLikesForPost(@Param("postId") String postId);

  @Delete("delete from posts where author_id = #{authorId} and id = #{postId}")
  int deletePostOwnedBy(@Param("authorId") String authorId, @Param("postId") String postId);

  @Select("select id from posts where id = #{postId} for update")
  String lockPost(@Param("postId") String postId);

  @Insert("insert into post_likes (post_id, user_id) values (#{postId}, #{userId})")
  int insertLike(@Param("postId") String postId, @Param("userId") String userId);

  @Delete("delete from post_likes where post_id = #{postId} and user_id = #{userId}")
  int deleteLike(@Param("postId") String postId, @Param("userId") String userId);

  @Update("update posts set likes = greatest(likes + #{delta}, 0) where id = #{postId}")
  int adjustLikes(@Param("postId") String postId, @Param("delta") int delta);

  @Select("""
      select cast(c.id as signed) as id,
             u.name as author,
             c.body,
             date_format(c.created_at, '%H:%i') as time,
             c.moderation_status as moderationStatus
      from comments c
      join users u on u.id = c.author_id
      where c.post_id = #{postId}
        and c.moderation_status = 'approved'
        and c.id regexp '^[0-9]+$'
      order by c.created_at, cast(c.id as signed)
      """)
  List<CommentEntity> findVisibleComments(@Param("postId") String postId);

  @Select("""
      select cast(c.id as signed) as id,
             u.name as author,
             c.body,
             date_format(c.created_at, '%H:%i') as time,
             c.moderation_status as moderationStatus
      from comments c
      join users u on u.id = c.author_id
      where c.post_id = #{postId} and c.id = #{commentId}
      """)
  CommentEntity findComment(@Param("postId") String postId, @Param("commentId") String commentId);

  @Insert("""
      insert into comments (id, post_id, author_id, body, moderation_status)
      values (#{id}, #{postId}, #{authorId}, #{body}, 'pending')
      """)
  void insertComment(
      @Param("id") String id,
      @Param("postId") String postId,
      @Param("authorId") String authorId,
      @Param("body") String body);

  @Update("update posts set moderation_status = #{status} where id = #{postId}")
  void updatePostModeration(@Param("postId") String postId, @Param("status") String status);

  @Update("""
      update comments
      set moderation_status = #{status}
      where post_id = #{postId} and id = #{commentId}
      """)
  void updateCommentModeration(
      @Param("postId") String postId,
      @Param("commentId") String commentId,
      @Param("status") String status);
}
