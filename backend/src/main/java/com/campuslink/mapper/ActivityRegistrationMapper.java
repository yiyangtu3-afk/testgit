package com.campuslink.mapper;

import com.campuslink.entity.ActivityRegistrationEntity;
import com.campuslink.entity.ActivityRegistrationEventEntity;
import com.campuslink.entity.ActivityRosterEntryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityRegistrationMapper {

  @Select("""
      select id, activity_id as activityId, attendee_id as attendeeId, status,
             registered_at as registeredAt, waitlisted_at as waitlistedAt,
             checked_in_at as checkedInAt, cancelled_at as cancelledAt, created_at as createdAt
      from activity_registrations
      where activity_id = #{activityId} and attendee_id = #{attendeeId}
      for update
      """)
  ActivityRegistrationEntity findForUpdate(
      @Param("activityId") String activityId, @Param("attendeeId") String attendeeId);

  @Select("""
      select id, activity_id as activityId, attendee_id as attendeeId, status,
             registered_at as registeredAt, waitlisted_at as waitlistedAt,
             checked_in_at as checkedInAt, cancelled_at as cancelledAt, created_at as createdAt
      from activity_registrations
      where activity_id = #{activityId} and attendee_id = #{attendeeId}
      """)
  ActivityRegistrationEntity find(
      @Param("activityId") String activityId, @Param("attendeeId") String attendeeId);

  @Select("""
      select id, activity_id as activityId, attendee_id as attendeeId, status,
             registered_at as registeredAt, waitlisted_at as waitlistedAt,
             checked_in_at as checkedInAt, cancelled_at as cancelledAt, created_at as createdAt
      from activity_registrations
      where activity_id = #{activityId} and id = #{registrationId}
      for update
      """)
  ActivityRegistrationEntity findByIdForUpdate(
      @Param("activityId") String activityId, @Param("registrationId") String registrationId);

  @Select("""
      select count(*) from activity_registrations
      where activity_id = #{activityId} and status in ('registered', 'checked_in')
      """)
  int countOccupied(@Param("activityId") String activityId);

  @Select("""
      select count(*) from activity_registrations
      where activity_id = #{activityId} and status = 'waitlisted'
      """)
  int countWaitlisted(@Param("activityId") String activityId);

  @Select("select count(*) from activity_registrations where status in ('registered', 'checked_in')")
  int countAllOccupied();

  @Select("select count(*) from activity_registrations where status = 'checked_in'")
  int countAllCheckedIn();

  @Select("""
      select count(*)
      from activity_registrations candidate
      join activity_registrations current on current.id = #{registrationId}
      where candidate.activity_id = current.activity_id
        and candidate.status = 'waitlisted'
        and (candidate.waitlisted_at < current.waitlisted_at
          or (candidate.waitlisted_at = current.waitlisted_at and candidate.id <= current.id))
      """)
  int queuePosition(@Param("registrationId") String registrationId);

  @Select("""
      select id, activity_id as activityId, attendee_id as attendeeId, status,
             registered_at as registeredAt, waitlisted_at as waitlistedAt,
             checked_in_at as checkedInAt, cancelled_at as cancelledAt, created_at as createdAt
      from activity_registrations
      where activity_id = #{activityId} and status = 'waitlisted'
      order by waitlisted_at, id limit 1 for update
      """)
  ActivityRegistrationEntity findFirstWaitlistedForUpdate(@Param("activityId") String activityId);

  @Insert("""
      insert into activity_registrations (
        id, activity_id, attendee_id, status, registered_at, waitlisted_at
      ) values (
        #{id}, #{activityId}, #{attendeeId}, #{status},
        if(#{status} = 'registered', current_timestamp, null),
        if(#{status} = 'waitlisted', current_timestamp, null)
      )
      """)
  void insert(@Param("id") String id, @Param("activityId") String activityId,
      @Param("attendeeId") String attendeeId, @Param("status") String status);

  @Update("""
      update activity_registrations
      set status = #{status},
          registered_at = if(#{status} = 'registered', current_timestamp, registered_at),
          waitlisted_at = if(#{status} = 'waitlisted', current_timestamp, waitlisted_at),
          checked_in_at = if(#{status} = 'checked_in', current_timestamp, checked_in_at),
          cancelled_at = if(#{status} = 'cancelled', current_timestamp, null)
      where id = #{registrationId}
      """)
  int updateStatus(@Param("registrationId") String registrationId, @Param("status") String status);

  @Insert("""
      insert into activity_registration_events (
        id, registration_id, activity_id, attendee_id, actor_id, event_type, from_status, to_status
      ) values (
        #{id}, #{registrationId}, #{activityId}, #{attendeeId}, #{actorId},
        #{eventType}, #{fromStatus}, #{toStatus}
      )
      """)
  void insertEvent(@Param("id") String id, @Param("registrationId") String registrationId,
      @Param("activityId") String activityId, @Param("attendeeId") String attendeeId,
      @Param("actorId") String actorId, @Param("eventType") String eventType,
      @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

  @Select("""
      select id, registration_id as registrationId, activity_id as activityId,
             attendee_id as attendeeId, actor_id as actorId, event_type as eventType,
             from_status as fromStatus, to_status as toStatus, created_at as createdAt
      from activity_registration_events where activity_id = #{activityId}
      order by created_at, id
      """)
  List<ActivityRegistrationEventEntity> findEvents(@Param("activityId") String activityId);

  @Select("""
      select r.id as registrationId, r.activity_id as activityId,
             r.attendee_id as attendeeId, u.name as attendeeName, r.status,
             r.registered_at as registeredAt, r.waitlisted_at as waitlistedAt,
             r.checked_in_at as checkedInAt
      from activity_registrations r
      join users u on u.id = r.attendee_id
      where r.activity_id = #{activityId}
        and r.status in ('registered', 'waitlisted', 'checked_in')
      order by case when r.status = 'waitlisted' then 1 else 0 end,
               coalesce(r.waitlisted_at, r.registered_at), r.id
      """)
  List<ActivityRosterEntryEntity> findRoster(@Param("activityId") String activityId);
}
