package com.kstrinadka.securebankapi.service;

import com.kstrinadka.securebankapi.dto.response.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {

    TransferResponse transfer(Long fromUserId, Long toUserId, BigDecimal amount);
}
