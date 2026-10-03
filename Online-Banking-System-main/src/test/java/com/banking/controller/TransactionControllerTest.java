package com.banking.controller;

import com.banking.dto.AccountDto;
import com.banking.dto.TransactionDto;
import com.banking.service.AccountService;
import com.banking.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesTransactionHistoryAndFilterValues() {
        AccountDto account = new AccountDto();
        List<TransactionDto> transactions = List.of(new TransactionDto());
        when(accountService.getAccountDtoByEmail("customer@example.test")).thenReturn(account);
        when(transactionService.filterUserTransactions("customer@example.test", "weekly", "TXN-1"))
                .thenReturn(transactions);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/transactions",
                new TransactionController(transactionService, accountService)
                        .viewTransactionHistory("weekly", "TXN-1", model));

        assertEquals(account, model.getAttribute("account"));
        assertEquals(transactions, model.getAttribute("transactions"));
        assertEquals("weekly", model.getAttribute("currentTimeframe"));
        assertEquals("TXN-1", model.getAttribute("searchQuery"));
        verify(transactionService).filterUserTransactions("customer@example.test", "weekly", "TXN-1");
    }
}
