package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.config.CacheNames;
import com.kstrinadka.securebankapi.entity.UserEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findById(Long id);

    @Override
    @Cacheable(cacheNames = CacheNames.USER_EXISTS, key = "#p0")
    boolean existsById(Long id);
}
