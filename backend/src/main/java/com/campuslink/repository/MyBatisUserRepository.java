package com.campuslink.repository;

import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisUserRepository implements UserRepository {

  private final UserMapper userMapper;

  public MyBatisUserRepository(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public List<UserEntity> findAll() {
    return userMapper.findAll();
  }

  @Override
  public Optional<UserEntity> findById(String userId) {
    return Optional.ofNullable(userMapper.findById(userId));
  }

  @Override
  public Optional<UserEntity> findByPhone(String phone) {
    return Optional.ofNullable(userMapper.findByPhone(phone));
  }

  @Override
  public void updatePresence(String userId, String presence) {
    userMapper.updatePresence(userId, presence);
  }
}
