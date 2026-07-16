package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.mapper.FriendMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisFriendRepository implements FriendRepository {

  private final FriendMapper friendMapper;

  public MyBatisFriendRepository(FriendMapper friendMapper) {
    this.friendMapper = friendMapper;
  }

  @Override
  public boolean areFriends(String firstUserId, String secondUserId) {
    List<String> pair = orderedPair(firstUserId, secondUserId);
    return friendMapper.countFriendship(pair.get(0), pair.get(1)) > 0;
  }

  @Override
  public List<String> findFriendIdsForUser(String userId) {
    return friendMapper.findFriendIdsForUser(userId);
  }

  @Override
  public void addFriendship(String firstUserId, String secondUserId) {
    List<String> pair = orderedPair(firstUserId, secondUserId);
    friendMapper.addFriendship(pair.get(0), pair.get(1));
  }

  @Override
  public FriendRequestEntity upsertFriendRequest(String fromUserId, String toUserId, String status) {
    String existingRequestId = friendMapper.findExistingRequestId(fromUserId, toUserId);
    if (existingRequestId != null) {
      updateRequestStatus(existingRequestId, status);
      return new FriendRequestEntity(existingRequestId, fromUserId, toUserId, status);
    }
    String requestId = "fr-" + System.currentTimeMillis();
    friendMapper.createFriendRequest(requestId, fromUserId, toUserId, status);
    return new FriendRequestEntity(requestId, fromUserId, toUserId, status);
  }

  @Override
  public List<FriendRequestEntity> findRequestsForUser(String userId) {
    return friendMapper.findRequestsForUser(userId);
  }

  @Override
  public Optional<FriendRequestEntity> findRequestForRecipient(String requestId, String recipientUserId) {
    return Optional.ofNullable(friendMapper.findRequestForRecipient(requestId, recipientUserId));
  }

  @Override
  public void updateRequestStatus(String requestId, String status) {
    friendMapper.updateRequestStatus(requestId, status);
  }

  private List<String> orderedPair(String firstUserId, String secondUserId) {
    if (firstUserId.compareTo(secondUserId) <= 0) {
      return List.of(firstUserId, secondUserId);
    }
    return List.of(secondUserId, firstUserId);
  }
}
