package com.campuslink.service;

import com.campuslink.dto.DemoDtos.CodeResponse;
import com.campuslink.dto.DemoDtos.CurrentUser;
import com.campuslink.dto.DemoDtos.LoginResponse;
import com.campuslink.entity.DemoEntities.UserEntity;
import com.campuslink.repository.UserRepository;
import com.campuslink.repository.VerificationCodeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final VerificationCodeRepository verificationCodeRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final AuthTokenService authTokenService;

  public AuthService(
      VerificationCodeRepository verificationCodeRepository,
      UserRepository userRepository,
      AuditService auditService,
      AuthTokenService authTokenService) {
    this.verificationCodeRepository = verificationCodeRepository;
    this.userRepository = userRepository;
    this.auditService = auditService;
    this.authTokenService = authTokenService;
  }

  public CodeResponse createCode(String phone) {
    String code = String.valueOf((int) (Math.random() * 900000) + 100000);
    verificationCodeRepository.save(phone, code);
    return new CodeResponse(phone, code);
  }

  public LoginResponse login(String phone, String code) {
    boolean matched = verificationCodeRepository.findByPhone(phone)
        .filter(code::equals)
        .isPresent();
    if (!matched) {
      throw new IllegalArgumentException("验证码不正确");
    }
    UserEntity user = userRepository.findByPhone(phone)
        .orElseThrow(() -> new IllegalArgumentException("手机号未注册演示账号"));
    auditService.addAudit("用户", phone + " 登录成功");
    return loginResponseFor(user);
  }

  @Transactional
  public LoginResponse register(String name, String phone, String code) {
    boolean matched = verificationCodeRepository.findByPhone(phone)
        .filter(code::equals)
        .isPresent();
    if (!matched) {
      throw new IllegalArgumentException("验证码不正确");
    }
    if (userRepository.findByPhone(phone).isPresent()) {
      throw new ConflictException("该手机号已注册，请直接登录");
    }
    UserEntity user = userRepository.saveNewUser(new UserEntity(
        UUID.randomUUID().toString().replace("-", ""),
        name.trim(),
        "学生账号",
        phone,
        "online"));
    auditService.addAudit("用户", user.name() + " 注册成功");
    return loginResponseFor(user);
  }

  public LoginResponse demoLogin(String userId) {
    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("演示账号不存在"));
    auditService.addAudit("用户", user.phone() + " 快速进入演示");
    return loginResponseFor(user);
  }

  public void logout(String authorization) {
    authTokenService.logout(authorization);
  }

  private LoginResponse loginResponseFor(UserEntity user) {
    return new LoginResponse(
        authTokenService.issueToken(user.id()),
        new CurrentUser(user.id(), user.name(), user.role(), user.phone()));
  }
}
