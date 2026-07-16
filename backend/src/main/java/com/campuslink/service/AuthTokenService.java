package com.campuslink.service;

import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.AuthSessionRepository;
import com.campuslink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private final AuthSessionRepository authSessionRepository;
  private final UserRepository userRepository;
  private final JwtTokenCodec jwtTokens;

  @Autowired
  public AuthTokenService(
      AuthSessionRepository authSessionRepository,
      UserRepository userRepository,
      JwtTokenCodec jwtTokens) {
    this.authSessionRepository = authSessionRepository;
    this.userRepository = userRepository;
    this.jwtTokens = jwtTokens;
  }

  public AuthTokenService(AuthSessionRepository authSessionRepository, UserRepository userRepository) {
    this(authSessionRepository, userRepository,
        new JwtTokenCodec("campuslink-test-signing-secret-must-have-32-bytes", 3600,
            java.time.Clock.systemUTC()));
  }

  public String issueToken(String userId) {
    String token = jwtTokens.issue(userId);
    authSessionRepository.save(token, userId);
    return token;
  }

  public String requireUserId(String authorization) {
    UserEntity authenticatedUser = authenticatedUser();
    if (authenticatedUser != null) {
      return authenticatedUser.id();
    }
    return requireToken(bearerToken(authorization));
  }

  public String requireToken(String token) {
    if (token == null || token.isBlank()) {
      throw new SecurityException("请先登录");
    }
    String subject = jwtTokens.requireSubject(token);
    String userId = authSessionRepository.findUserIdByToken(token)
        .orElseThrow(() -> new SecurityException("登录已失效，请重新登录"));
    if (!subject.equals(userId)) {
      throw new SecurityException("登录令牌无效");
    }
    return userId;
  }

  public void logout(String authorization) {
    authSessionRepository.delete(bearerToken(authorization));
  }

  public UserEntity requireUser(String authorization) {
    UserEntity authenticatedUser = authenticatedUser();
    if (authenticatedUser != null) {
      return authenticatedUser;
    }
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

  private UserEntity authenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserEntity user) {
      return user;
    }
    return null;
  }
}
