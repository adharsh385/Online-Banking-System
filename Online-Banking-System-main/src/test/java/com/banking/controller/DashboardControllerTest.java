package com.banking.controller;

import com.banking.dto.DashboardSummaryDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", null, java.util.List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsCustomerDashboardSummaryToModel() {
        DashboardSummaryDto summary = new DashboardSummaryDto();
        when(transactionService.getDashboardSummaryForUser("customer@example.test")).thenReturn(summary);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("customer/dashboard", new DashboardController(transactionService).customerDashboard(model));

        assertEquals(summary, model.getAttribute("dashboard"));
        verify(transactionService).getDashboardSummaryForUser("customer@example.test");
    }
}
