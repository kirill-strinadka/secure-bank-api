package com.kstrinadka.securebankapi.unit;

import com.kstrinadka.securebankapi.dto.request.EmailCreateRequest;
import com.kstrinadka.securebankapi.dto.request.PhoneCreateRequest;
import com.kstrinadka.securebankapi.dto.request.TransferRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationDtoTest extends AbstractUnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validEmailPassesValidation() {
        EmailCreateRequest request = new EmailCreateRequest("ivan@mail.com");

        Set<ConstraintViolation<EmailCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankEmailFailsValidation() {
        EmailCreateRequest request = new EmailCreateRequest("");

        Set<ConstraintViolation<EmailCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void invalidEmailFailsValidation() {
        EmailCreateRequest request = new EmailCreateRequest("not-an-email");

        Set<ConstraintViolation<EmailCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void validPhonePassesValidation() {
        PhoneCreateRequest request = new PhoneCreateRequest("79207865431");

        Set<ConstraintViolation<PhoneCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidPhoneFailsValidation() {
        PhoneCreateRequest request = new PhoneCreateRequest("89207865431");

        Set<ConstraintViolation<PhoneCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void zeroTransferAmountFailsValidation() {
        TransferRequest request = new TransferRequest(2L, BigDecimal.ZERO);

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void transferAmountWithThreeFractionDigitsFailsValidation() {
        TransferRequest request = new TransferRequest(2L, new BigDecimal("10.001"));

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
