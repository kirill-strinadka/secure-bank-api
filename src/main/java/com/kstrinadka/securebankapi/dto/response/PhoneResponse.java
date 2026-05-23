package com.kstrinadka.securebankapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneResponse {

    private Long id;

    private String phone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
