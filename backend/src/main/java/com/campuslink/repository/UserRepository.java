package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.UserEntity;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  List<UserEntity> findAll();

  Optional<UserEntity> findById(String userId);

  Optional<UserEntity> findByPhone(String phone);

  default UserEntity saveNewUser(UserEntity user) {
    throw new UnsupportedOperationException("当前用户仓库不支持注册");
  }

  void updatePresence(String userId, String presence);
}
