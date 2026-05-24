package com.kstrinadka.securebankapi.controller;

import com.kstrinadka.securebankapi.dto.request.EmailCreateRequest;
import com.kstrinadka.securebankapi.dto.request.EmailUpdateRequest;
import com.kstrinadka.securebankapi.dto.response.EmailResponse;
import com.kstrinadka.securebankapi.service.EmailService;
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
@RequestMapping("/api/v1/users/me/emails")
@RequiredArgsConstructor
@Tag(name = "Emails", description = "Current user email management")
public class EmailController {

    private final EmailService emailService;

    @GetMapping
    public List<EmailResponse> getCurrentUserEmails() {
        return emailService.getCurrentUserEmails();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmailResponse addEmail(@Valid @RequestBody EmailCreateRequest request) {
        return emailService.addEmail(request);
    }

    @PutMapping("/{emailId}")
    public EmailResponse updateEmail(
            @PathVariable Long emailId,
            @Valid @RequestBody EmailUpdateRequest request
    ) {
        return emailService.updateEmail(emailId, request);
    }

    @DeleteMapping("/{emailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmail(@PathVariable Long emailId) {
        emailService.deleteEmail(emailId);
    }
}
