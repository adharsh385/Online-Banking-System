package com.banking.service.impl;

import com.banking.constants.AccountStatus;
import com.banking.constants.AccountType;
import com.banking.constants.TransactionStatus;
import com.banking.constants.TransactionType;
import com.banking.dto.DepositWithdrawDto;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.exception.AccountFrozenException;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogService auditLogService;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, transactionRepository, auditLogService);
        ReflectionTestUtils.setField(accountService, "ifscPrefix", "APEX");
        ReflectionTestUtils.setField(accountService, "defaultBranch", "Central Branch");
    }

    @Test
    void createsAccountAndRecordsPositiveInitialDeposit() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User owner = new User();
        BigDecimal initialDeposit = new BigDecimal("750.00");

        Account account = accountService.createAccountForUser(owner, AccountType.CURRENT, initialDeposit);

        assertTrue(account.getAccountNumber().matches("1000\\d{8}"));
        assertEquals(initialDeposit, account.getBalance());
        assertEquals(AccountType.CURRENT, account.getAccountType());
        assertEquals(AccountStatus.ACTIVE, account.getAccountStatus());
        assertEquals("Central Branch", account.getBranch());
        assertEquals("APEX101", account.getIfscCode());
        assertSame(owner, account.getUser());

        ArgumentCaptor<Transaction> transaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertEquals(TransactionType.DEPOSIT, transaction.getValue().getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, transaction.getValue().getStatus());
        assertEquals(initialDeposit, transaction.getValue().getAmount());
        assertSame(account, transaction.getValue().getReceiverAccount());
    }

    @Test
    void defaultsNullDepositAndAccountTypeWithoutCreatingTransaction() {
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccountForUser(new User(), null, null);

        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void normalizesEmailWhenLookingUpAccount() {
        Account expected = new Account();
        when(accountRepository.findByUserEmail("customer@example.test")).thenReturn(Optional.of(expected));

        assertSame(expected, accountService.getAccountByUserEmail(" CUSTOMER@EXAMPLE.TEST "));
    }

    @Test
    void throwsWhenAccountEmailDoesNotExist() {
        when(accountRepository.findByUserEmail("missing@example.test")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByUserEmail("missing@example.test"));
    }

    @Test
    void depositsFundsAndRecordsDepositTransaction() {
        Account account = account(new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(accountRepository.findByUserEmail("customer@example.test")).thenReturn(Optional.of(account));
        DepositWithdrawDto request = request(TransactionType.DEPOSIT, "25.00", null);

        accountService.depositOrWithdraw("customer@example.test", request);

        assertEquals(new BigDecimal("125.00"), account.getBalance());
        ArgumentCaptor<Transaction> transaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertEquals(TransactionType.DEPOSIT, transaction.getValue().getTransactionType());
        assertEquals("DEPOSIT", transaction.getValue().getRemarks());
        assertSame(account, transaction.getValue().getReceiverAccount());
        verify(auditLogService).logAction("customer@example.test", "DEPOSIT",
                "DEPOSIT of $25.00 processed successfully", "127.0.0.1");
    }

    @Test
    void withdrawsFundsAndRecordsWithdrawalTransaction() {
        Account account = account(new BigDecimal("100.00"), AccountStatus.ACTIVE);
        when(accountRepository.findByUserEmail("customer@example.test")).thenReturn(Optional.of(account));

        accountService.depositOrWithdraw("customer@example.test",
                request(TransactionType.WITHDRAWAL, "40.00", "Cash"));

        assertEquals(new BigDecimal("60.00"), account.getBalance());
        ArgumentCaptor<Transaction> transaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertSame(account, transaction.getValue().getSenderAccount());
        assertEquals(TransactionStatus.SUCCESS, transaction.getValue().getStatus());
        assertEquals("Cash", transaction.getValue().getRemarks());
    }

    @Test
    void rejectsFrozenAccountWithoutPersistingTransaction() {
        Account account = account(new BigDecimal("100.00"), AccountStatus.FROZEN);
        when(accountRepository.findByUserEmail("customer@example.test")).thenReturn(Optional.of(account));

        assertThrows(AccountFrozenException.class, () -> accountService.depositOrWithdraw(
                "customer@example.test", request(TransactionType.DEPOSIT, "25.00", null)));

        verifyNoInteractions(transactionRepository, auditLogService);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void rejectsWithdrawalAboveAvailableBalance() {
        Account account = account(new BigDecimal("10.00"), AccountStatus.ACTIVE);
        when(accountRepository.findByUserEmail("customer@example.test")).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class, () -> accountService.depositOrWithdraw(
                "customer@example.test", request(TransactionType.WITHDRAWAL, "11.00", null)));

        assertEquals(new BigDecimal("10.00"), account.getBalance());
        verifyNoInteractions(transactionRepository, auditLogService);
        verify(accountRepository, never()).save(any(Account.class));
    }

    private static Account account(BigDecimal balance, AccountStatus status) {
        Account account = new Account();
        account.setBalance(balance);
        account.setAccountStatus(status);
        return account;
    }

    private static DepositWithdrawDto request(TransactionType type, String amount, String remarks) {
        DepositWithdrawDto dto = new DepositWithdrawDto();
        dto.setTransactionType(type);
        dto.setAmount(new BigDecimal(amount));
        dto.setRemarks(remarks);
        return dto;
    }
}
