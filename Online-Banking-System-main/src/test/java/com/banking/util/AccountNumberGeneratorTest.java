package com.banking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountNumberGeneratorTest {

    @Test
    void generatedAccountNumberUsesTwelveDigitsAndBankPrefix() {
        String accountNumber = AccountNumberGenerator.generateAccountNumber();

        assertTrue(accountNumber.matches("1000\\d{8}"));
    }

    @Test
    void generatedTransactionIdContainsTimestampAndFourDigitSuffix() {
        String transactionId = AccountNumberGenerator.generateTransactionId();

        assertTrue(transactionId.matches("TXN-\\d+-\\d{4}"));
    }

    @Test
    void ifscCodePadsBranchCodeToAtLeastThreeDigits() {
        assertEquals("APEX007", AccountNumberGenerator.generateIfscCode("APEX", 7));
        assertEquals("APEX1234", AccountNumberGenerator.generateIfscCode("APEX", 1234));
    }
}
