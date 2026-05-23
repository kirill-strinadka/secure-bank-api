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
public class EmailResponse {

    private Long id;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
