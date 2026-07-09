package com.campuslink.repository;

import java.util.Optional;

public interface VerificationCodeRepository {

  void save(String phone, String code);

  Optional<String> findByPhone(String phone);
}
