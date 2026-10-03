package com.banking.service.impl;

import com.banking.constants.AccountStatus;
import com.banking.constants.TransactionStatus;
import com.banking.constants.TransactionType;
import com.banking.dto.TransferRequestDto;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.exception.AccountFrozenException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.AccountService;
import com.banking.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogService auditLogService;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                accountService, accountRepository, transactionRepository, auditLogService);
    }

    @Test
    void transfersFundsAndPersistsSuccessfulTransaction() {
        Account sender = account(1L, "100000000001", "100.00", AccountStatus.ACTIVE);
        Account receiver = account(2L, "100000000002", "20.00", AccountStatus.ACTIVE);
        when(accountService.getAccountByUserEmail("sender@example.test")).thenReturn(sender);
        when(accountService.getAccountByNumber(receiver.getAccountNumber())).thenReturn(receiver);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        TransferRequestDto request = request(receiver.getAccountNumber(), "35.00", "Rent");

        Transaction result = transferService.transferFunds("sender@example.test", request);

        assertEquals(new BigDecimal("65.00"), sender.getBalance());
        assertEquals(new BigDecimal("55.00"), receiver.getBalance());
        assertEquals(TransactionType.TRANSFER, result.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(new BigDecimal("35.00"), result.getAmount());
        assertEquals("Rent", result.getRemarks());
        assertSame(sender, result.getSenderAccount());
        assertSame(receiver, result.getReceiverAccount());
        verify(accountRepository).save(sender);
        verify(accountRepository).save(receiver);
        verify(auditLogService).logAction(
                "sender@example.test", "FUND_TRANSFER",
                "Transferred $35.00 to Account #100000000002", "127.0.0.1");
    }

    @Test
    void rejectsSelfTransferWithoutPersistingChanges() {
        Account sender = account(1L, "100000000001", "100.00", AccountStatus.ACTIVE);
        Account sameAccount = account(1L, "100000000001", "100.00", AccountStatus.ACTIVE);
        when(accountService.getAccountByUserEmail("sender@example.test")).thenReturn(sender);
        when(accountService.getAccountByNumber(sender.getAccountNumber())).thenReturn(sameAccount);

        assertThrows(InvalidTransactionException.class, () ->
                transferService.transferFunds("sender@example.test",
                        request(sender.getAccountNumber(), "10.00", null)));

        verifyNoInteractions(accountRepository, transactionRepository, auditLogService);
    }

    @Test
    void rejectsTransferFromFrozenSender() {
        Account sender = account(1L, "100000000001", "100.00", AccountStatus.FROZEN);
        Account receiver = account(2L, "100000000002", "20.00", AccountStatus.ACTIVE);
        when(accountService.getAccountByUserEmail("sender@example.test")).thenReturn(sender);
        when(accountService.getAccountByNumber(receiver.getAccountNumber())).thenReturn(receiver);

        assertThrows(AccountFrozenException.class, () ->
                transferService.transferFunds("sender@example.test",
                        request(receiver.getAccountNumber(), "10.00", null)));

        verifyNoInteractions(accountRepository, transactionRepository, auditLogService);
    }

    @Test
    void rejectsTransferToInactiveReceiver() {
        Account sender = account(1L, "100000000001", "100.00", AccountStatus.ACTIVE);
        Account receiver = account(2L, "100000000002", "20.00", AccountStatus.INACTIVE);
        when(accountService.getAccountByUserEmail("sender@example.test")).thenReturn(sender);
        when(accountService.getAccountByNumber(receiver.getAccountNumber())).thenReturn(receiver);

        assertThrows(AccountFrozenException.class, () ->
                transferService.transferFunds("sender@example.test",
                        request(receiver.getAccountNumber(), "10.00", null)));

        verifyNoInteractions(accountRepository, transactionRepository, auditLogService);
    }

    @Test
    void rejectsTransferWhenBalanceIsInsufficient() {
        Account sender = account(1L, "100000000001", "10.00", AccountStatus.ACTIVE);
        Account receiver = account(2L, "100000000002", "20.00", AccountStatus.ACTIVE);
        when(accountService.getAccountByUserEmail("sender@example.test")).thenReturn(sender);
        when(accountService.getAccountByNumber(receiver.getAccountNumber())).thenReturn(receiver);

        assertThrows(InsufficientBalanceException.class, () ->
                transferService.transferFunds("sender@example.test",
                        request(receiver.getAccountNumber(), "11.00", null)));

        assertEquals(new BigDecimal("10.00"), sender.getBalance());
        assertEquals(new BigDecimal("20.00"), receiver.getBalance());
        verifyNoInteractions(accountRepository, transactionRepository, auditLogService);
    }

    private static Account account(Long id, String number, String balance, AccountStatus status) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(number);
        account.setBalance(new BigDecimal(balance));
        account.setAccountStatus(status);
        return account;
    }

    private static TransferRequestDto request(String recipient, String amount, String remarks) {
        TransferRequestDto request = new TransferRequestDto();
        request.setRecipientAccountNumber(recipient);
        request.setAmount(new BigDecimal(amount));
        request.setRemarks(remarks);
        return request;
    }
}
