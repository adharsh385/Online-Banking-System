package com.banking.security;

import com.banking.constants.RoleName;
import com.banking.entity.Role;
import com.banking.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void exposesUserIdentityAndGrantedRoles() {
        User user = user(true);
        user.setRoles(Set.of(new Role(RoleName.ROLE_CUSTOMER), new Role(RoleName.ROLE_ADMIN)));
        CustomUserDetails details = new CustomUserDetails(user);

        assertEquals("customer@example.test", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        assertEquals("Avery Morgan", details.getFullName());
        assertSame(user, details.getUser());
        assertEquals(Set.of(
                new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")), Set.copyOf(details.getAuthorities()));
    }

    @Test
    void enabledUserIsNotLocked() {
        CustomUserDetails details = new CustomUserDetails(user(true));

        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void disabledUserIsNeitherEnabledNorUnlocked() {
        CustomUserDetails details = new CustomUserDetails(user(false));

        assertFalse(details.isEnabled());
        assertFalse(details.isAccountNonLocked());
    }

    private static User user(boolean enabled) {
        User user = new User();
        user.setFirstName("Avery");
        user.setLastName("Morgan");
        user.setEmail("customer@example.test");
        user.setPassword("encoded-password");
        user.setEnabled(enabled);
        return user;
    }
}
