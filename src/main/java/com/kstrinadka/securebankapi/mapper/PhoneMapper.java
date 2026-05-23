package com.kstrinadka.securebankapi.mapper;

import com.kstrinadka.securebankapi.dto.response.PhoneResponse;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import org.springframework.stereotype.Component;

@Component
public class PhoneMapper {

    public PhoneResponse toResponse(PhoneDataEntity phoneData) {
        return new PhoneResponse(
                phoneData.getId(),
                phoneData.getPhone(),
                phoneData.getCreatedAt(),
                phoneData.getUpdatedAt()
        );
    }
}
