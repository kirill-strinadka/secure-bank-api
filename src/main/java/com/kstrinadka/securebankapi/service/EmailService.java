package com.kstrinadka.securebankapi.service;

import com.kstrinadka.securebankapi.dto.request.EmailCreateRequest;
import com.kstrinadka.securebankapi.dto.request.EmailUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.EmailResponse;

import java.util.List;

public interface EmailService {

    List<EmailResponse> getCurrentUserEmails();

    EmailResponse addEmail(EmailCreateRequest request);

    EmailResponse updateEmail(Long emailId, EmailUpdateRequest request);

    void deleteEmail(Long emailId);
}
