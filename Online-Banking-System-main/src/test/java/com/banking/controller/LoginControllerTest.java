package com.banking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginControllerTest {

    private final LoginController controller = new LoginController();

    @Test
    void returnsLoginPageForAnonymousUser() {
        assertEquals("public/login", controller.loginPage(null, null, null, new ExtendedModelMap()));
    }

    @Test
    void addsErrorAndLogoutMessagesToLoginPage() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("public/login", controller.loginPage("failed", "true", null, model));
        assertEquals("Invalid email address or password. Please check your credentials.",
                model.getAttribute("errorMessage"));
        assertEquals("You have been successfully logged out.", model.getAttribute("successMessage"));
    }

    @Test
    void redirectsAuthenticatedAdministratorToAdminDashboard() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertEquals("redirect:/admin/dashboard",
                controller.loginPage(null, null, authentication, new ExtendedModelMap()));
    }

    @Test
    void redirectsAuthenticatedCustomerToCustomerDashboard() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        assertEquals("redirect:/customer/dashboard",
                controller.loginPage(null, null, authentication, new ExtendedModelMap()));
    }
}
