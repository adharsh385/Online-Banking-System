package com.banking.service.impl;

import com.banking.constants.AccountStatus;
import com.banking.constants.AccountType;
import com.banking.constants.TransactionType;
import com.banking.dto.DashboardSummaryDto;
import com.banking.dto.TransactionDto;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.repository.TransactionRepository;
import com.banking.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountService accountService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, accountService);
    }

    @Test
    void mapsTransactionHistoryForCurrentAccount() {
        Account account = account(1L, "100000000001");
        Transaction deposit = transaction(null, account, "25.00", TransactionType.DEPOSIT);
        when(accountService.getAccountByUserEmail("customer@example.test")).thenReturn(account);
        when(transactionRepository.findBySenderAccountOrReceiverAccountOrderByTimestampDesc(account, account))
                .thenReturn(List.of(deposit));

        List<TransactionDto> history = transactionService.getTransactionHistoryForUser("customer@example.test");

        assertEquals(1, history.size());
        assertTrue(history.get(0).isCredit());
        assertEquals("SYSTEM / DEPOSIT", history.get(0).getSenderAccountNumber());
        verify(transactionRepository).findBySenderAccountOrReceiverAccountOrderByTimestampDesc(account, account);
    }

    @Test
    void searchesOnlyTransactionsBelongingToCurrentAccountAndTrimsQuery() {
        Account current = account(1L, "100000000001");
        Account other = account(2L, "100000000002");
        Transaction belongsToCurrent = transaction(current, other, "8.00", TransactionType.TRANSFER);
        Transaction unrelated = transaction(other, account(3L, "100000000003"), "9.00", TransactionType.TRANSFER);
        when(accountService.getAccountByUserEmail("customer@example.test")).thenReturn(current);
        when(transactionRepository.findByTransactionIdContainingIgnoreCase("TXN-42"))
                .thenReturn(List.of(belongsToCurrent, unrelated));

        List<TransactionDto> results =
                transactionService.filterUserTransactions("customer@example.test", null, " TXN-42 ");

        assertEquals(1, results.size());
        assertEquals(belongsToCurrent.getTransactionId(), results.get(0).getTransactionId());
        verify(transactionRepository).findByTransactionIdContainingIgnoreCase("TXN-42");
        verify(transactionRepository, never()).findByAccountAndDateRange(any(), any(), any());
    }

    @Test
    void filtersTransactionsFromStartOfCurrentDay() {
        Account account = account(1L, "100000000001");
        when(accountService.getAccountByUserEmail("customer@example.test")).thenReturn(account);
        when(transactionRepository.findByAccountAndDateRange(eq(account), any(), any())).thenReturn(List.of());
        LocalDate today = LocalDate.now();

        List<TransactionDto> results =
                transactionService.filterUserTransactions("customer@example.test", "today", null);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository).findByAccountAndDateRange(eq(account), start.capture(), end.capture());
        assertTrue(start.getValue().equals(today.atStartOfDay())
                || start.getValue().equals(today.plusDays(1).atStartOfDay()));
        assertFalse(end.getValue().isBefore(start.getValue()));
        assertTrue(results.isEmpty());
    }

    @Test
    void summarizesMoneyInOutAndLimitsRecentTransactionsToFive() {
        Account account = account(1L, "100000000001");
        User owner = new User();
        owner.setFirstName("Avery");
        owner.setLastName("Morgan");
        account.setUser(owner);
        account.setBalance(new BigDecimal("75.00"));
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);

        List<Transaction> transactions = List.of(
                transaction(null, account, "50.00", TransactionType.DEPOSIT),
                transaction(account, null, "12.00", TransactionType.WITHDRAWAL),
                transaction(account, null, "5.00", TransactionType.WITHDRAWAL),
                transaction(null, account, "3.00", TransactionType.DEPOSIT),
                transaction(account, null, "2.00", TransactionType.TRANSFER),
                transaction(null, account, "1.00", TransactionType.DEPOSIT));
        when(accountService.getAccountByUserEmail("customer@example.test")).thenReturn(account);
        when(transactionRepository.findBySenderAccountOrReceiverAccountOrderByTimestampDesc(account, account))
                .thenReturn(transactions);

        DashboardSummaryDto summary =
                transactionService.getDashboardSummaryForUser("customer@example.test");

        assertEquals("Avery Morgan", summary.getCustomerName());
        assertEquals(new BigDecimal("54.00"), summary.getTotalMoneyIn());
        assertEquals(new BigDecimal("19.00"), summary.getTotalMoneyOut());
        assertEquals(6, summary.getTotalTransactionsCount());
        assertEquals(5, summary.getRecentTransactions().size());
    }

    private static Account account(Long id, String number) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(number);
        return account;
    }

    private static Transaction transaction(Account sender, Account receiver, String amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-42");
        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionType(type);
        return transaction;
    }
}
