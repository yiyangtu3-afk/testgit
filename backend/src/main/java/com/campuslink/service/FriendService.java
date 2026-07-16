package com.campuslink.service;

import com.campuslink.dto.DemoDtos.FriendRequestResponse;
import com.campuslink.dto.DemoDtos.FriendRequestView;
import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.entity.DemoEntities.FriendRequestEntity;
import com.campuslink.repository.ChatRepository;
import com.campuslink.repository.FriendRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

  private final FriendRepository friendRepository;
  private final ChatRepository chatRepository;
  private final UserService userService;
  private final AuditService auditService;
  private final SocialNotificationService notifications;

  public FriendService(
      FriendRepository friendRepository,
      ChatRepository chatRepository,
      UserService userService,
      AuditService auditService,
      SocialNotificationService notifications) {
    this.friendRepository = friendRepository;
    this.chatRepository = chatRepository;
    this.userService = userService;
    this.auditService = auditService;
    this.notifications = notifications;
  }

  @Transactional
  public FriendRequestResponse createFriendRequest(String fromUserId, String toUserId) {
    String activeUserId = userService.activeUserId(fromUserId);
    if (areFriends(activeUserId, toUserId)) {
      return new FriendRequestResponse(toUserId, "accepted");
    }
    FriendRequestEntity request = updateFriendRequest(activeUserId, toUserId, "pending");
    notifications.recordFriendRequestReceived(
        toUserId, activeUserId, userService.userName(activeUserId), request.id());
    auditService.addAudit("好友", userService.userName(activeUserId) + "向" + userService.userName(toUserId) + "发送好友申请");
    return new FriendRequestResponse(toUserId, "pending");
  }

  public List<UserView> friends(String currentUserId) {
    String activeUserId = userService.activeUserId(currentUserId);
    return friendRepository.findFriendIdsForUser(activeUserId).stream()
        .map(userService::userViewById)
        .toList();
  }

  public List<FriendRequestView> friendRequests(String currentUserId) {
    String activeUserId = userService.activeUserId(currentUserId);
    return friendRepository.findRequestsForUser(activeUserId).stream()
        .map(request -> toFriendRequestView(request, activeUserId))
        .toList();
  }

  @Transactional
  public FriendRequestView acceptFriendRequest(String requestId, String currentUserId) {
    String activeUserId = userService.activeUserId(currentUserId);
    FriendRequestEntity request = resolveFriendRequest(requestId, activeUserId, "accepted");
    addFriendship(request.fromUserId(), request.toUserId());
    chatRepository.saveMessage(
        request.fromUserId(),
        activeUserId,
        "我们已经是好友了，之后可以直接在这里沟通。",
        List.of());
    chatRepository.saveMessage(
        request.toUserId(),
        activeUserId,
        "我们已经是好友了，之后可以直接在这里沟通。",
        List.of());
    notifications.recordFriendRequestResolved(
        request.fromUserId(),
        activeUserId,
        userService.userName(activeUserId),
        request.id(),
        request.status());
    auditService.addAudit("好友", userService.userName(activeUserId) + "同意" + userService.userName(request.fromUserId()) + "的好友申请");
    return toFriendRequestView(request, activeUserId);
  }

  @Transactional
  public FriendRequestView rejectFriendRequest(String requestId, String currentUserId) {
    String activeUserId = userService.activeUserId(currentUserId);
    FriendRequestEntity request = resolveFriendRequest(requestId, activeUserId, "rejected");
    notifications.recordFriendRequestResolved(
        request.fromUserId(),
        activeUserId,
        userService.userName(activeUserId),
        request.id(),
        request.status());
    auditService.addAudit("好友", userService.userName(activeUserId) + "拒绝" + userService.userName(request.fromUserId()) + "的好友申请");
    return toFriendRequestView(request, activeUserId);
  }

  private boolean areFriends(String firstUserId, String secondUserId) {
    return friendRepository.areFriends(firstUserId, secondUserId);
  }

  private void addFriendship(String firstUserId, String secondUserId) {
    friendRepository.addFriendship(firstUserId, secondUserId);
  }

  private FriendRequestEntity updateFriendRequest(String fromUserId, String toUserId, String status) {
    return friendRepository.upsertFriendRequest(fromUserId, toUserId, status);
  }

  private FriendRequestEntity resolveFriendRequest(String requestId, String currentUserId, String status) {
    FriendRequestEntity request = friendRepository.findRequestForRecipient(requestId, currentUserId)
        .orElseThrow(() -> new IllegalArgumentException("好友申请不存在"));
    friendRepository.updateRequestStatus(requestId, status);
    return new FriendRequestEntity(request.id(), request.fromUserId(), request.toUserId(), status);
  }

  private FriendRequestView toFriendRequestView(FriendRequestEntity request, String currentUserId) {
    boolean incoming = request.toUserId().equals(currentUserId);
    return DemoMapper.toFriendRequestView(
        request,
        currentUserId,
        userService.userViewById(incoming ? request.fromUserId() : request.toUserId()));
  }
}
