package com.banking.service.impl;

import com.banking.constants.AccountType;
import com.banking.constants.RoleName;
import com.banking.dto.ChangePasswordDto;
import com.banking.dto.RegisterDto;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.exception.DuplicateEmailException;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.RoleRepository;
import com.banking.repository.UserRepository;
import com.banking.service.AccountService;
import com.banking.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository, roleRepository, accountService, passwordEncoder, auditLogService);
    }

    @Test
    void registersNormalizedCustomerAndCreatesAccount() {
        RegisterDto request = registration();
        Role customerRole = new Role(RoleName.ROLE_CUSTOMER);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(request);

        assertEquals("customer@example.test", registered.getEmail());
        assertEquals("encoded", registered.getPassword());
        assertTrue(registered.isEnabled());
        assertTrue(registered.getRoles().contains(customerRole));
        verify(accountService).createAccountForUser(
                registered, AccountType.SAVINGS, new BigDecimal("500.00"));
        verify(auditLogService).logAction("customer@example.test", "USER_REGISTERED",
                "New user registered with initial deposit $500.00", "127.0.0.1");
    }

    @Test
    void createsCustomerRoleWhenRoleIsNotPresent() {
        RegisterDto request = registration();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerUser(request);

        assertTrue(user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_CUSTOMER));
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void rejectsMismatchedRegistrationPasswords() {
        RegisterDto request = registration();
        request.setConfirmPassword("different");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));

        verifyNoInteractions(userRepository, roleRepository, accountService, passwordEncoder, auditLogService);
    }

    @Test
    void rejectsDuplicateRegistrationEmail() {
        RegisterDto request = registration();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.registerUser(request));

        verify(userRepository).existsByEmail(request.getEmail());
        verifyNoInteractions(roleRepository, accountService, passwordEncoder, auditLogService);
    }

    @Test
    void changesPasswordAfterValidatingCurrentAndConfirmation() {
        User user = user("customer@example.test", "old-encoded");
        when(userRepository.findByEmail("customer@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword!1", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword!2")).thenReturn("new-encoded");

        userService.changePassword("CUSTOMER@EXAMPLE.TEST", passwordChange(
                "OldPassword!1", "NewPassword!2", "NewPassword!2"));

        assertEquals("new-encoded", user.getPassword());
        verify(userRepository).save(user);
        verify(auditLogService).logAction("CUSTOMER@EXAMPLE.TEST", "PASSWORD_CHANGED",
                "User successfully changed password", "127.0.0.1");
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordIsWrong() {
        User user = user("customer@example.test", "old-encoded");
        when(userRepository.findByEmail("customer@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-encoded")).thenReturn(false);

        assertThrows(InvalidTransactionException.class, () -> userService.changePassword(
                "customer@example.test", passwordChange("wrong", "NewPassword!2", "NewPassword!2")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsPasswordChangeWhenConfirmationDoesNotMatch() {
        User user = user("customer@example.test", "old-encoded");
        when(userRepository.findByEmail("customer@example.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);

        assertThrows(InvalidTransactionException.class, () -> userService.changePassword(
                "customer@example.test", passwordChange("old", "NewPassword!2", "other")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void togglesUserEnabledStatus() {
        User user = user("customer@example.test", "encoded");
        user.setEnabled(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.toggleUserStatus(7L);

        assertFalse(user.isEnabled());
        verify(userRepository).save(user);
        verify(auditLogService).logAction("ADMIN", "USER_STATUS_TOGGLED",
                "User customer@example.test status set to DISABLED", "127.0.0.1");
    }

    private static RegisterDto registration() {
        RegisterDto request = new RegisterDto();
        request.setFirstName("Avery");
        request.setLastName("Morgan");
        request.setEmail(" CUSTOMER@EXAMPLE.TEST ");
        request.setPassword("Password!123");
        request.setConfirmPassword("Password!123");
        request.setPhone("1234567890");
        request.setAddress("Main Street");
        request.setAccountType(AccountType.SAVINGS);
        request.setInitialDeposit(new BigDecimal("500.00"));
        return request;
    }

    private static ChangePasswordDto passwordChange(String current, String next, String confirm) {
        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        dto.setConfirmPassword(confirm);
        return dto;
    }

    private static User user(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
