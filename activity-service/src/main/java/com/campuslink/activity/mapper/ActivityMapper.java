package com.campuslink.activity.mapper;

import com.campuslink.activity.domain.ActivityRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper {
  String FIELDS = "id, title, description, category, location, starts_at as startsAt, ends_at as endsAt, capacity, organizer_id as organizerId, status, review_decision as reviewDecision, review_reason as reviewReason, reviewed_by as reviewerId, reviewed_at as reviewedAt, created_at as createdAt";
  @Insert("insert into activities (id,title,description,category,location,starts_at,ends_at,capacity,organizer_id,status,review_decision) values (#{id},#{title},#{description},#{category},#{location},#{startsAt},#{endsAt},#{capacity},#{organizerId},'pending','pending')")
  void insert(@Param("id") String id, @Param("organizerId") String organizerId, @Param("title") String title, @Param("description") String description, @Param("category") String category, @Param("location") String location, @Param("startsAt") LocalDateTime startsAt, @Param("endsAt") LocalDateTime endsAt, @Param("capacity") int capacity);
  @Insert("insert into activity_reviews (id,activity_id,actor_id,decision,reason) values (#{id},#{activityId},#{actorId},#{decision},#{reason})")
  void insertReview(@Param("id") String id, @Param("activityId") String activityId, @Param("actorId") String actorId, @Param("decision") String decision, @Param("reason") String reason);
  @Select("select " + FIELDS + " from activities where id=#{id}") ActivityRecord find(@Param("id") String id);
  @Select("select " + FIELDS + " from activities where id=#{id} for update") ActivityRecord findForUpdate(@Param("id") String id);
  @Select("<script>select " + FIELDS + " from activities where status in ('published','full') <if test='category != null'>and category=#{category}</if> <if test='from != null'>and starts_at &gt;= #{from}</if> <if test='before != null'>and starts_at &lt; #{before}</if> order by starts_at,created_at desc</script>")
  List<ActivityRecord> published(@Param("category") String category, @Param("from") LocalDateTime from, @Param("before") LocalDateTime before);
  @Select("select " + FIELDS + " from activities where organizer_id=#{organizerId} order by created_at desc,id desc") List<ActivityRecord> managed(@Param("organizerId") String organizerId);
  @Select("select " + FIELDS + " from activities where status='pending' and review_decision='pending' order by created_at,id") List<ActivityRecord> pending();
  @Update("update activities set status=#{status},review_decision=#{decision},review_reason=#{reason},reviewed_by=#{reviewerId},reviewed_at=current_timestamp where id=#{id} and status='pending' and review_decision='pending'")
  int review(@Param("id") String id, @Param("status") String status, @Param("decision") String decision, @Param("reason") String reason, @Param("reviewerId") String reviewerId);
  @Update("update activities set status=#{status} where id=#{id} and status in ('published','full')")
  int updateRegistrationStatus(@Param("id") String id, @Param("status") String status);
}
