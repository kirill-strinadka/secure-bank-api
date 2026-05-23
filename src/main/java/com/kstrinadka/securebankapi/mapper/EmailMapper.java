package com.kstrinadka.securebankapi.mapper;

import com.kstrinadka.securebankapi.dto.response.EmailResponse;
import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import org.springframework.stereotype.Component;

@Component
public class EmailMapper {

    public EmailResponse toResponse(EmailDataEntity emailData) {
        return new EmailResponse(
                emailData.getId(),
                emailData.getEmail(),
                emailData.getCreatedAt(),
                emailData.getUpdatedAt()
        );
    }
}
