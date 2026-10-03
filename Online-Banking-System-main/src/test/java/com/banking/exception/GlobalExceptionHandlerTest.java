package com.banking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void redirectsMissingAccountToDashboard() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/dashboard",
                handler.handleAccountNotFound(new AccountNotFoundException("missing"), redirect));
        assertEquals("missing", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void redirectsInsufficientBalanceToTransferForm() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/transfer",
                handler.handleInsufficientBalance(new InsufficientBalanceException("low balance"), redirect));
        assertEquals("low balance", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void redirectsFrozenAccountToDashboard() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/dashboard",
                handler.handleAccountFrozen(new AccountFrozenException("frozen"), redirect));
        assertEquals("frozen", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void redirectsInvalidTransactionToTransferForm() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/transfer",
                handler.handleInvalidTransaction(new InvalidTransactionException("invalid"), redirect));
        assertEquals("invalid", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void redirectsDuplicateEmailToRegistration() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/register",
                handler.handleDuplicateEmail(new DuplicateEmailException("duplicate"), redirect));
        assertEquals("duplicate", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void returnsServerErrorViewAndFallbackMessageForMessageLessException() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("error/500", handler.handleGeneralException(new RuntimeException(), model));
        assertEquals(500, model.getAttribute("status"));
        assertEquals("Internal Server Error", model.getAttribute("error"));
        assertEquals("An unexpected error occurred.", model.getAttribute("message"));
    }
}
