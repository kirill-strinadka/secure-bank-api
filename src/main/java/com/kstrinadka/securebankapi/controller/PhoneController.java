package com.kstrinadka.securebankapi.controller;

import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.PhoneResponse;
import com.kstrinadka.securebankapi.service.PhoneService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/phones")
@RequiredArgsConstructor
@Tag(name = "Phones", description = "Current user phone management")
public class PhoneController {

    private final PhoneService phoneService;

    @GetMapping
    public List<PhoneResponse> getCurrentUserPhones() {
        return phoneService.getCurrentUserPhones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PhoneResponse addPhone(@Valid @RequestBody PhoneCreateRequest request) {
        return phoneService.addPhone(request);
    }

    @PutMapping("/{phoneId}")
    public PhoneResponse updatePhone(
            @PathVariable Long phoneId,
            @Valid @RequestBody PhoneUpdateRequest request
    ) {
        return phoneService.updatePhone(phoneId, request);
    }

    @DeleteMapping("/{phoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhone(@PathVariable Long phoneId) {
        phoneService.deletePhone(phoneId);
    }
}
