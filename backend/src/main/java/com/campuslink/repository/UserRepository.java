package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.UserEntity;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  List<UserEntity> findAll();

  Optional<UserEntity> findById(String userId);

  Optional<UserEntity> findByPhone(String phone);

  void updatePresence(String userId, String presence);
}
