package com.campuslink.service;

import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private final SecureRandom random = new SecureRandom();
  private final AuthSessionRepository authSessionRepository;
  private final UserRepository userRepository;

  public AuthTokenService(AuthSessionRepository authSessionRepository, UserRepository userRepository) {
    this.authSessionRepository = authSessionRepository;
    this.userRepository = userRepository;
  }

  public String issueToken(String userId) {
    byte[] bytes = new byte[24];
    random.nextBytes(bytes);
    String token = "demo." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    authSessionRepository.save(token, userId);
    return token;
  }

  public String requireUserId(String authorization) {
    return requireToken(bearerToken(authorization));
  }

  public String requireToken(String token) {
    if (token == null || token.isBlank()) {
      throw new SecurityException("请先登录");
    }
    return authSessionRepository.findUserIdByToken(token)
        .orElseThrow(() -> new SecurityException("登录已失效，请重新登录"));
  }

  public UserEntity requireUser(String authorization) {
    String userId = requireUserId(authorization);
    return userRepository.findById(userId)
        .orElseThrow(() -> new SecurityException("当前用户不存在"));
  }

  public UserEntity requireAdmin(String authorization) {
    UserEntity user = requireUser(authorization);
    if (!user.role().contains("管理员")) {
      throw new ForbiddenException("需要管理员账号");
    }
    return user;
  }

  private String bearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new SecurityException("请先登录");
    }
    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      throw new SecurityException("请先登录");
    }
    return token;
  }
}
