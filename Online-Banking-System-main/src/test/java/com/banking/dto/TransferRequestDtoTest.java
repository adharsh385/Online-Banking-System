package com.banking.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransferRequestDtoTest {

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
    void acceptsValidTransferRequest() {
        TransferRequestDto request = request("100020003001", new BigDecimal("1.00"));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankRecipientAccountNumber() {
        TransferRequestDto request = request(" ", new BigDecimal("25.00"));

        Set<String> properties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(properties.contains("recipientAccountNumber"));
    }

    @Test
    void rejectsAmountBelowMinimum() {
        TransferRequestDto request = request("100020003001", new BigDecimal("0.99"));

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void rejectsAmountAboveMaximum() {
        TransferRequestDto request = request("100020003001", new BigDecimal("100000.01"));

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("amount")));
    }

    private static TransferRequestDto request(String recipient, BigDecimal amount) {
        TransferRequestDto request = new TransferRequestDto();
        request.setRecipientAccountNumber(recipient);
        request.setAmount(amount);
        return request;
    }
}
