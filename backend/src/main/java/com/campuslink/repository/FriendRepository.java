package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import java.util.List;
import java.util.Optional;

public interface FriendRepository {

  boolean areFriends(String firstUserId, String secondUserId);

  List<String> findFriendIdsForUser(String userId);

  void addFriendship(String firstUserId, String secondUserId);

  FriendRequestEntity upsertFriendRequest(String fromUserId, String toUserId, String status);

  List<FriendRequestEntity> findRequestsForUser(String userId);

  Optional<FriendRequestEntity> findRequestForRecipient(String requestId, String recipientUserId);

  void updateRequestStatus(String requestId, String status);
}
