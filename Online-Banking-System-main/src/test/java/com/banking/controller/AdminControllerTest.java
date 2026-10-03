package com.banking.controller;

import com.banking.constants.AccountStatus;
import com.banking.dto.AccountDto;
import com.banking.dto.AdminReportDto;
import com.banking.dto.TransactionDto;
import com.banking.entity.AuditLog;
import com.banking.entity.User;
import com.banking.service.AccountService;
import com.banking.service.AdminService;
import com.banking.service.AuditLogService;
import com.banking.service.TransactionService;
import com.banking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;
    @Mock
    private UserService userService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private AuditLogService auditLogService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(
                adminService, userService, accountService, transactionService, auditLogService);
    }

    @Test
    void dashboardLimitsRecentTransactionsToTen() {
        when(adminService.generateAdminReport()).thenReturn(new AdminReportDto());
        when(accountService.getAllAccounts()).thenReturn(List.of(new AccountDto()));
        List<TransactionDto> transactions = java.util.stream.IntStream.range(0, 12)
                .mapToObj(ignored -> new TransactionDto()).toList();
        when(transactionService.getAllTransactions()).thenReturn(transactions);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/dashboard", controller.adminDashboard(model));
        assertEquals(10, ((List<?>) model.getAttribute("recentTransactions")).size());
        verify(adminService).generateAdminReport();
    }

    @Test
    void searchesTransactionsWhenQueryIsPresentAndNonBlank() {
        when(transactionService.searchTransactionsById("TXN-8")).thenReturn(List.of(new TransactionDto()));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/transactions", controller.manageTransactions(" TXN-8 ", model));
        verify(transactionService).searchTransactionsById("TXN-8");
        verify(transactionService, never()).getAllTransactions();
    }

    @Test
    void listsAllTransactionsForBlankQuery() {
        when(transactionService.getAllTransactions()).thenReturn(List.of());

        assertEquals("admin/transactions", controller.manageTransactions(" ", new ExtendedModelMap()));

        verify(transactionService).getAllTransactions();
        verify(transactionService, never()).searchTransactionsById(anyString());
    }

    @Test
    void freezesAccountAndSetsSuccessMessage() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/admin/accounts", controller.freezeAccount(3L, redirect));

        verify(accountService).updateAccountStatus(3L, AccountStatus.FROZEN);
        assertEquals("Account successfully FROZEN.", redirect.getFlashAttributes().get("successMessage"));
    }

    @Test
    void activatesAccount() {
        assertEquals("redirect:/admin/accounts",
                controller.activateAccount(3L, new RedirectAttributesModelMap()));

        verify(accountService).updateAccountStatus(3L, AccountStatus.ACTIVE);
    }

    @Test
    void loadsUsersForManagementView() {
        List<User> users = List.of(new User());
        when(userService.getAllUsers()).thenReturn(users);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/users", controller.manageUsers(model));
        assertEquals(users, model.getAttribute("users"));
    }

    @Test
    void togglesUserStatusAndRedirectsToUsers() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/admin/users", controller.toggleUserStatus(6L, redirect));

        verify(userService).toggleUserStatus(6L);
        assertEquals("User access status updated successfully!",
                redirect.getFlashAttributes().get("successMessage"));
    }

    @Test
    void loadsAccountListForManagementView() {
        List<AccountDto> accounts = List.of(new AccountDto());
        when(accountService.getAllAccounts()).thenReturn(accounts);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/accounts", controller.manageAccounts(model));
        assertEquals(accounts, model.getAttribute("accounts"));
    }

    @Test
    void loadsReportForReportsView() {
        AdminReportDto report = new AdminReportDto();
        when(adminService.generateAdminReport()).thenReturn(report);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/reports", controller.viewReports(model));
        assertEquals(report, model.getAttribute("report"));
    }

    @Test
    void loadsRecentAuditLogsForAuditView() {
        List<AuditLog> logs = List.of(new AuditLog());
        when(auditLogService.getRecentAuditLogs()).thenReturn(logs);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/audit-logs", controller.viewAuditLogs(model));
        assertEquals(logs, model.getAttribute("auditLogs"));
    }
}
