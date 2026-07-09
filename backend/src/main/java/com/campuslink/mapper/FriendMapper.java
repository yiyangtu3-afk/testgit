package com.campuslink.mapper;

import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FriendMapper {

  @Select("""
      select count(*)
      from friendships
      where first_user_id = #{firstUserId} and second_user_id = #{secondUserId}
      """)
  int countFriendship(@Param("firstUserId") String firstUserId, @Param("secondUserId") String secondUserId);

  @Select("""
      select case
        when first_user_id = #{userId} then second_user_id
        else first_user_id
      end
      from friendships
      where first_user_id = #{userId} or second_user_id = #{userId}
      order by created_at, id
      """)
  List<String> findFriendIdsForUser(@Param("userId") String userId);

  @Insert("""
      insert ignore into friendships (first_user_id, second_user_id)
      values (#{firstUserId}, #{secondUserId})
      """)
  void addFriendship(@Param("firstUserId") String firstUserId, @Param("secondUserId") String secondUserId);

  @Select("""
      select id
      from friend_requests
      where from_user_id = #{fromUserId} and to_user_id = #{toUserId}
      order by created_at desc, id desc
      limit 1
      """)
  String findExistingRequestId(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);

  @Insert("""
      insert into friend_requests (id, from_user_id, to_user_id, status)
      values (#{id}, #{fromUserId}, #{toUserId}, #{status})
      """)
  void createFriendRequest(
      @Param("id") String id,
      @Param("fromUserId") String fromUserId,
      @Param("toUserId") String toUserId,
      @Param("status") String status);

  @Select("""
      select id, from_user_id as fromUserId, to_user_id as toUserId, status
      from friend_requests
      where from_user_id = #{userId} or to_user_id = #{userId}
      order by created_at desc, id desc
      """)
  List<FriendRequestEntity> findRequestsForUser(@Param("userId") String userId);

  @Select("""
      select id, from_user_id as fromUserId, to_user_id as toUserId, status
      from friend_requests
      where id = #{requestId} and to_user_id = #{recipientUserId}
      """)
  FriendRequestEntity findRequestForRecipient(
      @Param("requestId") String requestId,
      @Param("recipientUserId") String recipientUserId);

  @Update("update friend_requests set status = #{status} where id = #{requestId}")
  void updateRequestStatus(@Param("requestId") String requestId, @Param("status") String status);
}
