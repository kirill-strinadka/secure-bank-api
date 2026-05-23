package com.kstrinadka.securebankapi.service;

import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.PhoneResponse;

import java.util.List;

public interface PhoneService {

    List<PhoneResponse> getCurrentUserPhones();

    PhoneResponse addPhone(PhoneCreateRequest request);

    PhoneResponse updatePhone(Long phoneId, PhoneUpdateRequest request);

    void deletePhone(Long phoneId);
}
