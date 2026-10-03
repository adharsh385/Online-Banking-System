package com.banking.controller;

import com.banking.constants.TransactionType;
import com.banking.dto.AccountDto;
import com.banking.dto.ChangePasswordDto;
import com.banking.dto.DepositWithdrawDto;
import com.banking.dto.UserProfileDto;
import com.banking.entity.User;
import com.banking.service.AccountService;
import com.banking.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;
    @Mock
    private UserService userService;
    @Mock
    private BindingResult bindingResult;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(accountService, userService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", null, java.util.List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadsAccountDetailsForAuthenticatedCustomer() {
        AccountDto account = new AccountDto();
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(account);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/account-details", controller.viewAccountDetails(model));
        assertSame(account, model.getAttribute("account"));
    }

    @Test
    void loadsProfileAndMapsUserWhenNoFormObjectExists() {
        User user = new User();
        user.setFirstName("Avery");
        user.setLastName("Morgan");
        when(userService.findByEmail("customer@example.test")).thenReturn(user);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/profile", controller.viewProfile(model));
        UserProfileDto profile = (UserProfileDto) model.getAttribute("profileDto");
        assertEquals("Avery", profile.getFirstName());
        assertEquals("Morgan", profile.getLastName());
    }

    @Test
    void updateProfileReturnsFormWhenValidationFails() {
        when(bindingResult.hasErrors()).thenReturn(true);

        assertEquals("customer/profile",
                controller.updateProfile(new UserProfileDto(), bindingResult, new RedirectAttributesModelMap()));

        verifyNoInteractions(userService);
    }

    @Test
    void updatesValidProfileAndRedirects() {
        when(bindingResult.hasErrors()).thenReturn(false);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/profile",
                controller.updateProfile(new UserProfileDto(), bindingResult, redirect));

        verify(userService).updateProfile(eq("customer@example.test"), any(UserProfileDto.class));
        assertEquals("Profile details updated successfully!", redirect.getFlashAttributes().get("successMessage"));
    }

    @Test
    void initializesChangePasswordForm() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/change-password", controller.showChangePasswordForm(model));
        assertNotNull(model.getAttribute("changePasswordDto"));
    }

    @Test
    void displaysPasswordServiceErrorAsFlashMessage() {
        when(bindingResult.hasErrors()).thenReturn(false);
        ChangePasswordDto request = new ChangePasswordDto();
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword("customer@example.test", request);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/change-password",
                controller.changePassword(request, bindingResult, redirect));
        assertEquals("Current password is incorrect", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void displaysDepositFormWithAccountAndRequest() {
        AccountDto account = new AccountDto();
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(account);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/deposit-withdraw", controller.showDepositWithdrawForm(model));
        assertSame(account, model.getAttribute("account"));
        assertNotNull(model.getAttribute("depositWithdrawDto"));
    }

    @Test
    void processesDepositAndRedirectsToDashboard() {
        AccountDto account = new AccountDto();
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(account);
        when(bindingResult.hasErrors()).thenReturn(false);
        DepositWithdrawDto request = depositRequest();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/dashboard", controller.processDepositWithdraw(
                request, bindingResult, redirect, new ExtendedModelMap()));

        verify(accountService).depositOrWithdraw("customer@example.test", request);
        assertEquals("DEPOSIT of $25.00 processed successfully!",
                redirect.getFlashAttributes().get("successMessage"));
    }

    @Test
    void returnsDepositFormWhenRequestHasValidationErrors() {
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(new AccountDto());
        when(bindingResult.hasErrors()).thenReturn(true);

        assertEquals("customer/deposit-withdraw", controller.processDepositWithdraw(
                depositRequest(), bindingResult, new RedirectAttributesModelMap(), new ExtendedModelMap()));

        verify(accountService, never()).depositOrWithdraw(anyString(), any(DepositWithdrawDto.class));
    }

    @Test
    void displaysDepositServiceFailureAsFlashMessage() {
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(new AccountDto());
        when(bindingResult.hasErrors()).thenReturn(false);
        DepositWithdrawDto request = depositRequest();
        doThrow(new IllegalStateException("Account frozen"))
                .when(accountService).depositOrWithdraw("customer@example.test", request);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/customer/deposit-withdraw", controller.processDepositWithdraw(
                request, bindingResult, redirect, new ExtendedModelMap()));
        assertEquals("Account frozen", redirect.getFlashAttributes().get("errorMessage"));
    }

    private static DepositWithdrawDto depositRequest() {
        DepositWithdrawDto request = new DepositWithdrawDto();
        request.setTransactionType(TransactionType.DEPOSIT);
        request.setAmount(new BigDecimal("25.00"));
        return request;
    }
}
