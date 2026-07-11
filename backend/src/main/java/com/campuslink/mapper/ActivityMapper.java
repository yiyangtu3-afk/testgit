package com.campuslink.mapper;

import com.campuslink.entity.ActivityEntity;
import com.campuslink.entity.ActivityReviewEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper {

  String ACTIVITY_SELECT = """
      select a.id,
             a.title,
             a.description,
             a.category,
             a.location,
             a.starts_at as startsAt,
             a.ends_at as endsAt,
             a.capacity,
             a.organizer_id as organizerId,
             organizer.name as organizerName,
             a.status,
             a.review_decision as reviewDecision,
             a.review_reason as reviewReason,
             a.reviewed_by as reviewerId,
             reviewer.name as reviewerName,
             a.reviewed_at as reviewedAt,
             a.created_at as createdAt
      from activities a
      join users organizer on organizer.id = a.organizer_id
      left join users reviewer on reviewer.id = a.reviewed_by
      """;

  @Insert("""
      insert into activities (
        id, title, description, category, location, starts_at, ends_at,
        capacity, organizer_id, status, review_decision
      ) values (
        #{id}, #{title}, #{description}, #{category}, #{location}, #{startsAt},
        #{endsAt}, #{capacity}, #{organizerId}, 'pending', 'pending'
      )
      """)
  void insertActivity(
      @Param("id") String id,
      @Param("organizerId") String organizerId,
      @Param("title") String title,
      @Param("description") String description,
      @Param("category") String category,
      @Param("location") String location,
      @Param("startsAt") LocalDateTime startsAt,
      @Param("endsAt") LocalDateTime endsAt,
      @Param("capacity") int capacity);

  @Insert("""
      insert into activity_reviews (id, activity_id, actor_id, decision, reason)
      values (#{id}, #{activityId}, #{actorId}, #{decision}, #{reason})
      """)
  void insertReview(
      @Param("id") String id,
      @Param("activityId") String activityId,
      @Param("actorId") String actorId,
      @Param("decision") String decision,
      @Param("reason") String reason);

  @Select(ACTIVITY_SELECT + " where a.id = #{activityId}")
  ActivityEntity findById(@Param("activityId") String activityId);

  @Select(ACTIVITY_SELECT
      + " where a.status = 'published' order by a.starts_at, a.created_at desc")
  List<ActivityEntity> findPublished();

  @Select(ACTIVITY_SELECT
      + " where a.status = 'pending' and a.review_decision = 'pending'"
      + " order by a.created_at, a.id")
  List<ActivityEntity> findPending();

  @Update("""
      update activities
      set status = #{status},
          review_decision = #{decision},
          review_reason = #{reason},
          reviewed_by = #{reviewerId},
          reviewed_at = current_timestamp
      where id = #{activityId}
        and status = 'pending'
        and review_decision = 'pending'
      """)
  int updateReview(
      @Param("activityId") String activityId,
      @Param("status") String status,
      @Param("decision") String decision,
      @Param("reason") String reason,
      @Param("reviewerId") String reviewerId);

  @Select("""
      select id,
             activity_id as activityId,
             actor_id as actorId,
             decision,
             reason,
             created_at as createdAt
      from activity_reviews
      where activity_id = #{activityId}
      order by created_at, id
      """)
  List<ActivityReviewEntity> findReviews(@Param("activityId") String activityId);
}
