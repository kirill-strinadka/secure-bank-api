package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhoneDataRepository extends JpaRepository<PhoneDataEntity, Long> {

    List<PhoneDataEntity> findAllByUserIdOrderByPhoneAsc(Long userId);

    Optional<PhoneDataEntity> findByPhone(String phone);

    boolean existsByPhone(String phone);

    long countByUserId(Long userId);

    Optional<PhoneDataEntity> findByIdAndUserId(Long id, Long userId);
}
