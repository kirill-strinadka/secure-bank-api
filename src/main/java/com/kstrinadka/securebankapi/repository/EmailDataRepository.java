package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.config.CacheNames;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailDataRepository extends JpaRepository<EmailDataEntity, Long> {

    List<EmailDataEntity> findAllByUserIdOrderByEmailAsc(Long userId);

    Optional<EmailDataEntity> findByEmail(String email);

    @Cacheable(cacheNames = CacheNames.EMAIL_EXISTS, key = "#p0")
    boolean existsByEmail(String email);

    long countByUserId(Long userId);

    Optional<EmailDataEntity> findByIdAndUserId(Long id, Long userId);
}
