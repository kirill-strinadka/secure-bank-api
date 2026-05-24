package com.kstrinadka.securebankapi.controller;

import com.kstrinadka.securebankapi.dto.request.TransferRequest;
import com.kstrinadka.securebankapi.dto.response.TransferResponse;
import com.kstrinadka.securebankapi.security.CurrentUserProvider;
import com.kstrinadka.securebankapi.service.TransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Money transfers between users")
public class TransferController {

    private final CurrentUserProvider currentUserProvider;
    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return transferService.transfer(currentUserId, request.getToUserId(), request.getAmount());
    }
}
