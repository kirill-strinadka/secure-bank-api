package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferEntity, Long> {
}
