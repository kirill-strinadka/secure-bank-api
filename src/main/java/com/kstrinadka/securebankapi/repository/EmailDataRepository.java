package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailDataRepository extends JpaRepository<EmailDataEntity, Long> {

    Optional<EmailDataEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByUserId(Long userId);

    Optional<EmailDataEntity> findByIdAndUserId(Long id, Long userId);
}
