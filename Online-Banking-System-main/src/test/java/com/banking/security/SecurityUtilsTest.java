package com.banking.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityUtilsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNullWhenNoAuthenticationExists() {
        assertNull(SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void returnsNullForUnauthenticatedPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", "password"));

        assertNull(SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void returnsUsernameFromUserDetailsPrincipal() {
        User principal = new User("customer@example.test", "password", AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertEquals("customer@example.test", SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void returnsStringPrincipalForAuthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "customer@example.test", null, AuthorityUtils.NO_AUTHORITIES));

        assertEquals("customer@example.test", SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void returnsNullForUnsupportedPrincipalType() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new Object(), null, AuthorityUtils.NO_AUTHORITIES));

        assertNull(SecurityUtils.getCurrentUserEmail());
    }
}
