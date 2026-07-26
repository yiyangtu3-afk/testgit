package com.campuslink.activity.mapper;

import com.campuslink.activity.domain.RegistrationRecord;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegistrationMapper {
  String FIELDS = "id,activity_id as activityId,attendee_id as attendeeId,status,registered_at as registeredAt,waitlisted_at as waitlistedAt,checked_in_at as checkedInAt,cancelled_at as cancelledAt,created_at as createdAt";
  @Select("select " + FIELDS + " from activity_registrations where activity_id=#{activityId} and attendee_id=#{attendeeId}") RegistrationRecord find(@Param("activityId") String activityId, @Param("attendeeId") String attendeeId);
  @Select("select " + FIELDS + " from activity_registrations where activity_id=#{activityId} and attendee_id=#{attendeeId} for update") RegistrationRecord findForUpdate(@Param("activityId") String activityId, @Param("attendeeId") String attendeeId);
  @Select("select " + FIELDS + " from activity_registrations where activity_id=#{activityId} and id=#{registrationId} for update") RegistrationRecord findByIdForUpdate(@Param("activityId") String activityId, @Param("registrationId") String registrationId);
  @Select("select count(*) from activity_registrations where activity_id=#{activityId} and status in ('registered','checked_in')") int occupied(@Param("activityId") String activityId);
  @Select("select count(*) from activity_registrations where status in ('registered','checked_in')") int allOccupied();
  @Select("select count(*) from activity_registrations where status='checked_in'") int allCheckedIn();
  @Select("select count(*) from activity_registrations candidate join activity_registrations current on current.id=#{id} where candidate.activity_id=current.activity_id and candidate.status='waitlisted' and (candidate.waitlisted_at < current.waitlisted_at or (candidate.waitlisted_at=current.waitlisted_at and candidate.id <= current.id))") int queuePosition(@Param("id") String id);
  @Select("select " + FIELDS + " from activity_registrations where activity_id=#{activityId} and status='waitlisted' order by waitlisted_at,id limit 1 for update") RegistrationRecord firstWaitlisted(@Param("activityId") String activityId);
  @Select("select " + FIELDS + " from activity_registrations where activity_id=#{activityId} and status in ('registered','waitlisted','checked_in') order by case when status='waitlisted' then 1 else 0 end,coalesce(waitlisted_at,registered_at),id") List<RegistrationRecord> roster(@Param("activityId") String activityId);
  @Insert("insert into activity_registrations (id,activity_id,attendee_id,status,registered_at,waitlisted_at) values (#{id},#{activityId},#{attendeeId},#{status},if(#{status}='registered',current_timestamp,null),if(#{status}='waitlisted',current_timestamp,null))") void insert(@Param("id") String id, @Param("activityId") String activityId, @Param("attendeeId") String attendeeId, @Param("status") String status);
  @Update("update activity_registrations set status=#{status},registered_at=if(#{status}='registered',current_timestamp,registered_at),waitlisted_at=if(#{status}='waitlisted',current_timestamp,waitlisted_at),checked_in_at=if(#{status}='checked_in',current_timestamp,checked_in_at),cancelled_at=if(#{status}='cancelled',current_timestamp,null) where id=#{id}") int status(@Param("id") String id, @Param("status") String status);
  @Insert("insert into activity_registration_events (id,registration_id,activity_id,attendee_id,actor_id,event_type,from_status,to_status) values (#{id},#{registrationId},#{activityId},#{attendeeId},#{actorId},#{eventType},#{fromStatus},#{toStatus})") void event(@Param("id") String id, @Param("registrationId") String registrationId, @Param("activityId") String activityId, @Param("attendeeId") String attendeeId, @Param("actorId") String actorId, @Param("eventType") String eventType, @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);
}
