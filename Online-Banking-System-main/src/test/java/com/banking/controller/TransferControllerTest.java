package com.banking.controller;

import com.banking.dto.AccountDto;
import com.banking.dto.TransferRequestDto;
import com.banking.entity.Transaction;
import com.banking.security.SecurityUtils;
import com.banking.service.AccountService;
import com.banking.service.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;
    @Mock
    private AccountService accountService;
    @Mock
    private BindingResult bindingResult;

    private TransferController controller;

    @BeforeEach
    void setUp() {
        controller = new TransferController(transferService, accountService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", null, java.util.List.of()));
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(new AccountDto());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void displaysTransferFormWithAccountAndRequest() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/transfer", controller.showTransferForm(model));
        assertEquals("customer@example.test", SecurityUtils.getCurrentUserEmail());
        org.junit.jupiter.api.Assertions.assertNotNull(model.getAttribute("account"));
        org.junit.jupiter.api.Assertions.assertNotNull(model.getAttribute("transferRequestDto"));
    }

    @Test
    void returnsFormAgainWhenRequestHasValidationErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);
        TransferRequestDto request = request("100020003002", "25.00");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.executeTransfer(request, bindingResult, redirect, new ExtendedModelMap());

        assertEquals("customer/transfer", view);
        verifyNoInteractions(transferService);
        org.junit.jupiter.api.Assertions.assertTrue(redirect.getFlashAttributes().isEmpty());
    }

    @Test
    void transfersAndRedirectsWithSuccessMessage() {
        when(bindingResult.hasErrors()).thenReturn(false);
        TransferRequestDto request = request("100020003002", "25.00");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.executeTransfer(request, bindingResult, redirect, new ExtendedModelMap());

        assertEquals("redirect:/customer/dashboard", view);
        assertEquals("Success! Transferred $25.00 to account #100020003002",
                redirect.getFlashAttributes().get("successMessage"));
        verify(transferService).transferFunds("customer@example.test", request);
    }

    @Test
    void redirectsBackAndShowsServiceError() {
        when(bindingResult.hasErrors()).thenReturn(false);
        TransferRequestDto request = request("100020003002", "25.00");
        when(transferService.transferFunds("customer@example.test", request))
                .thenThrow(new IllegalStateException("Target account unavailable"));
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.executeTransfer(request, bindingResult, redirect, new ExtendedModelMap());

        assertEquals("redirect:/customer/transfer", view);
        assertEquals("Target account unavailable", redirect.getFlashAttributes().get("errorMessage"));
    }

    private static TransferRequestDto request(String accountNumber, String amount) {
        TransferRequestDto request = new TransferRequestDto();
        request.setRecipientAccountNumber(accountNumber);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
