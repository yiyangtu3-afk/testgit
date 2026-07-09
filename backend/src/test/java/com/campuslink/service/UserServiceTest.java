package com.campuslink.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.dto.DemoDtos.UserView;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  private final UserService userService = new UserService(new InMemoryUserRepository(List.of(
      new UserEntity("u-1001", "林一", "学生账号", "13800000001", "online"),
      new UserEntity("u-2001", "陈老师", "教师", "13800000002", "online"),
      new UserEntity("u-2002", "周同学", "学生", "13800000003", "offline"))));

  @Test
  void searchUsersExcludesCurrentUserAndMatchesKeyword() {
    List<UserView> results = userService.searchUsers("老师", "u-1001");

    assertThat(results).extracting(UserView::id).containsExactly("u-2001");
  }

  @Test
  void userViewByIdFallsBackForUnknownDemoUser() {
    UserView user = userService.userViewById("u-missing");

    assertThat(user.name()).isEqualTo("u-missing");
    assertThat(user.status()).isEqualTo("offline");
  }

  private static final class InMemoryUserRepository implements UserRepository {

    private final List<UserEntity> users;

    private InMemoryUserRepository(List<UserEntity> users) {
      this.users = users;
    }

    @Override
    public List<UserEntity> findAll() {
      return users;
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
      return users.stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public Optional<UserEntity> findByPhone(String phone) {
      return users.stream().filter(user -> user.phone().equals(phone)).findFirst();
    }

    @Override
    public void updatePresence(String userId, String presence) {
    }
  }
}
