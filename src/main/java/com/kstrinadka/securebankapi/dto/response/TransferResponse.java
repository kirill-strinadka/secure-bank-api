package com.kstrinadka.securebankapi.dto.response;

import com.kstrinadka.securebankapi.entity.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private BigDecimal amount;

    private TransferStatus status;

    private LocalDateTime createdAt;
}
