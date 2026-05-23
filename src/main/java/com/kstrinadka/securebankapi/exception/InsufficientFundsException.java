package com.kstrinadka.securebankapi.exception;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }

    public InsufficientFundsException(String code, String message) {
        super(code, message);
    }
}
