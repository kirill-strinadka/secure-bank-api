package com.kstrinadka.securebankapi.exception;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
