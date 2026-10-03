package com.banking.dto;

import com.banking.constants.AccountStatus;
import com.banking.constants.AccountType;
import com.banking.constants.TransactionType;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityDtoMapperTest {

    @Test
    void returnsNullWhenMappingNullAccount() {
        assertNull(EntityDtoMapper.toAccountDto(null));
    }

    @Test
    void mapsAccountFieldsAndOwnerDetails() {
        User owner = new User();
        owner.setFirstName("Avery");
        owner.setLastName("Morgan");
        owner.setEmail("avery@example.test");

        Account account = new Account();
        account.setId(14L);
        account.setAccountNumber("100020003014");
        account.setBalance(new BigDecimal("125.40"));
        account.setAccountType(AccountType.SAVINGS);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setBranch("Central");
        account.setIfscCode("APEX014");
        account.setCreatedAt(LocalDateTime.of(2025, 2, 3, 4, 5));
        account.setUser(owner);

        AccountDto dto = EntityDtoMapper.toAccountDto(account);

        assertEquals(14L, dto.getId());
        assertEquals("100020003014", dto.getAccountNumber());
        assertEquals(new BigDecimal("125.40"), dto.getBalance());
        assertEquals(AccountType.SAVINGS, dto.getAccountType());
        assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
        assertEquals("Avery Morgan", dto.getOwnerName());
        assertEquals("avery@example.test", dto.getOwnerEmail());
    }

    @Test
    void mapsSystemDepositAndMarksItAsCreditForReceiver() {
        User owner = new User();
        owner.setFirstName("Avery");
        owner.setLastName("Morgan");

        Account receiver = new Account();
        receiver.setId(9L);
        receiver.setAccountNumber("100020003009");
        receiver.setUser(owner);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-1234-5678");
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setReceiverAccount(receiver);

        TransactionDto dto = EntityDtoMapper.toTransactionDto(transaction, receiver);

        assertEquals("SYSTEM / DEPOSIT", dto.getSenderAccountNumber());
        assertEquals("Apex Central Reserve", dto.getSenderName());
        assertEquals("100020003009", dto.getReceiverAccountNumber());
        assertEquals("Avery Morgan", dto.getReceiverName());
        assertTrue(dto.isCredit());
    }

    @Test
    void mapsNullTransactionAndNullUserToNull() {
        assertNull(EntityDtoMapper.toTransactionDto(null, null));
        assertNull(EntityDtoMapper.toUserProfileDto(null));
    }

    @Test
    void mapsUserProfileFields() {
        User user = new User();
        user.setId(8L);
        user.setFirstName("Avery");
        user.setLastName("Morgan");
        user.setEmail("avery@example.test");
        user.setPhone("5550100");
        user.setAddress("Main Street");

        UserProfileDto dto = EntityDtoMapper.toUserProfileDto(user);

        assertEquals(8L, dto.getId());
        assertEquals("Avery", dto.getFirstName());
        assertEquals("Morgan", dto.getLastName());
        assertEquals("avery@example.test", dto.getEmail());
        assertEquals("5550100", dto.getPhone());
        assertEquals("Main Street", dto.getAddress());
    }
}
