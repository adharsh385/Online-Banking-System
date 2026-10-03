package com.banking.service.impl;

import com.banking.constants.AccountStatus;
import com.banking.constants.TransactionType;
import com.banking.dto.AdminReportDto;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void reportExcludesAdminAndAggregatesBankTotals() {
        when(userRepository.count()).thenReturn(4L);
        when(accountRepository.count()).thenReturn(6L);
        when(accountRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(5L);
        when(accountRepository.countByAccountStatus(AccountStatus.FROZEN)).thenReturn(1L);
        when(transactionRepository.countByTimestampAfter(org.mockito.ArgumentMatchers.any()))
                .thenReturn(9L);
        when(accountRepository.getTotalBankDeposits()).thenReturn(new BigDecimal("1200.00"));
        when(transactionRepository.sumAmountByTransactionType(TransactionType.TRANSFER))
                .thenReturn(new BigDecimal("300.00"));
        when(transactionRepository.sumAmountByTransactionType(TransactionType.WITHDRAWAL))
                .thenReturn(new BigDecimal("80.00"));

        AdminReportDto report = new AdminServiceImpl(
                userRepository, accountRepository, transactionRepository).generateAdminReport();

        assertEquals(3L, report.getTotalCustomers());
        assertEquals(6L, report.getTotalAccounts());
        assertEquals(5L, report.getActiveAccounts());
        assertEquals(1L, report.getFrozenAccounts());
        assertEquals(9L, report.getTodayTransactionsCount());
        assertEquals(new BigDecimal("1200.00"), report.getTotalBankDeposits());
        assertEquals(new BigDecimal("300.00"), report.getTotalTransfersVolume());
        assertEquals(new BigDecimal("80.00"), report.getTotalWithdrawalsVolume());
    }

    @Test
    void usesZeroForEmptyUserCountAndNullBankTotals() {
        when(userRepository.count()).thenReturn(0L);
        when(accountRepository.count()).thenReturn(0L);
        when(accountRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(0L);
        when(accountRepository.countByAccountStatus(AccountStatus.FROZEN)).thenReturn(0L);
        when(transactionRepository.countByTimestampAfter(org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
        when(accountRepository.getTotalBankDeposits()).thenReturn(null);
        when(transactionRepository.sumAmountByTransactionType(TransactionType.TRANSFER)).thenReturn(null);
        when(transactionRepository.sumAmountByTransactionType(TransactionType.WITHDRAWAL)).thenReturn(null);

        AdminReportDto report = new AdminServiceImpl(
                userRepository, accountRepository, transactionRepository).generateAdminReport();

        assertEquals(0L, report.getTotalCustomers());
        assertEquals(BigDecimal.ZERO, report.getTotalBankDeposits());
        assertEquals(BigDecimal.ZERO, report.getTotalTransfersVolume());
        assertEquals(BigDecimal.ZERO, report.getTotalWithdrawalsVolume());
    }
}
