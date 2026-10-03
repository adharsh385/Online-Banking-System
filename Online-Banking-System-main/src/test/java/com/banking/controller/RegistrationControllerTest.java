package com.banking.controller;

import com.banking.dto.RegisterDto;
import com.banking.entity.User;
import com.banking.exception.DuplicateEmailException;
import com.banking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private BindingResult bindingResult;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new RegistrationController(userService);
    }

    @Test
    void displaysRegistrationFormAndPreservesExistingFormObject() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("public/register", controller.showRegistrationForm(model));
        org.junit.jupiter.api.Assertions.assertNotNull(model.getAttribute("registerDto"));

        RegisterDto existing = new RegisterDto();
        model.addAttribute("registerDto", existing);
        controller.showRegistrationForm(model);
        org.junit.jupiter.api.Assertions.assertSame(existing, model.getAttribute("registerDto"));
    }

    @Test
    void rejectsPasswordConfirmationMismatch() {
        RegisterDto request = registration();
        request.setConfirmPassword("different");
        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(true);

        assertEquals("public/register", controller.registerUserAccount(
                request, bindingResult, new RedirectAttributesModelMap(), new ExtendedModelMap()));

        verify(bindingResult).rejectValue(
                "confirmPassword", "error.registerDto", "Password and confirmation do not match.");
        verify(userService, never()).registerUser(request);
    }

    @Test
    void rejectsAlreadyRegisteredEmail() {
        RegisterDto request = registration();
        when(userService.existsByEmail(request.getEmail())).thenReturn(true);
        when(bindingResult.hasErrors()).thenReturn(true);

        assertEquals("public/register", controller.registerUserAccount(
                request, bindingResult, new RedirectAttributesModelMap(), new ExtendedModelMap()));

        verify(bindingResult).rejectValue(
                "email", "error.registerDto", "An account with this email address already exists.");
    }

    @Test
    void redirectsToLoginAfterSuccessfulRegistration() {
        RegisterDto request = registration();
        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.registerUser(request)).thenReturn(new User());
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/login",
                controller.registerUserAccount(request, bindingResult, redirect, new ExtendedModelMap()));

        org.junit.jupiter.api.Assertions.assertEquals(
                "Registration successful! Your new bank account has been created. Please log in.",
                redirect.getFlashAttributes().get("successMessage"));
    }

    @Test
    void handlesDuplicateEmailDetectedDuringRegistration() {
        RegisterDto request = registration();
        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.registerUser(request)).thenThrow(new DuplicateEmailException("Already registered"));

        assertEquals("public/register", controller.registerUserAccount(
                request, bindingResult, new RedirectAttributesModelMap(), new ExtendedModelMap()));

        verify(bindingResult).rejectValue("email", "error.registerDto", "Already registered");
    }

    @Test
    void addsErrorMessageWhenRegistrationFailsUnexpectedly() {
        RegisterDto request = registration();
        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.registerUser(request)).thenThrow(new IllegalStateException("Database unavailable"));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("public/register", controller.registerUserAccount(
                request, bindingResult, new RedirectAttributesModelMap(), model));
        assertEquals("Registration failed: Database unavailable", model.getAttribute("errorMessage"));
    }

    private static RegisterDto registration() {
        RegisterDto request = new RegisterDto();
        request.setEmail("new@example.test");
        request.setPassword("Password!123");
        request.setConfirmPassword("Password!123");
        return request;
    }
}
