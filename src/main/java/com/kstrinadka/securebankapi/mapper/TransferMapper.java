package com.kstrinadka.securebankapi.mapper;

import com.kstrinadka.securebankapi.dto.response.TransferResponse;
import com.kstrinadka.securebankapi.entity.TransferEntity;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toResponse(TransferEntity transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromUser().getId(),
                transfer.getToUser().getId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
