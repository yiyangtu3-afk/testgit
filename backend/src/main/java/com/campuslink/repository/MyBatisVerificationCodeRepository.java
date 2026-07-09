package com.campuslink.repository;

import com.campuslink.mapper.VerificationCodeMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisVerificationCodeRepository implements VerificationCodeRepository {

  private final VerificationCodeMapper verificationCodeMapper;

  public MyBatisVerificationCodeRepository(VerificationCodeMapper verificationCodeMapper) {
    this.verificationCodeMapper = verificationCodeMapper;
  }

  @Override
  public void save(String phone, String code) {
    verificationCodeMapper.save(phone, code);
  }

  @Override
  public Optional<String> findByPhone(String phone) {
    return Optional.ofNullable(verificationCodeMapper.findByPhone(phone));
  }
}
