package com.kstrinadka.securebankapi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneUpdateRequest {

    @NotBlank
    @Pattern(regexp = "^7\\d{10}$")
    private String phone;
}
