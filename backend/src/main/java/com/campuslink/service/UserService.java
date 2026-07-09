package com.campuslink.service;

import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<UserView> searchUsers(String keyword, String currentUserId) {
    String activeUserId = activeUserId(currentUserId);
    return userRepository.findAll().stream()
        .filter(user -> !user.id().equals(activeUserId))
        .filter(user -> keyword == null || keyword.isBlank()
            || user.name().contains(keyword)
            || user.phone().contains(keyword))
        .map(DemoMapper::toUserView)
        .toList();
  }

  public UserView userViewById(String userId) {
    return DemoMapper.toUserView(userById(userId));
  }

  public String userName(String userId) {
    return userById(userId).name();
  }

  public String activeUserId(String currentUserId) {
    if (currentUserId == null || currentUserId.isBlank()) {
      throw new IllegalArgumentException("当前用户不能为空");
    }
    return currentUserId;
  }

  private UserEntity userById(String userId) {
    return userRepository.findById(userId)
        .orElse(new UserEntity(userId, userId, "演示用户", "", "offline"));
  }
}
