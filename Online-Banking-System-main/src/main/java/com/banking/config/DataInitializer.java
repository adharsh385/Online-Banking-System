package com.banking.config;

import com.banking.constants.AccountStatus;
import com.banking.constants.AccountType;
import com.banking.constants.RoleName;
import com.banking.constants.TransactionStatus;
import com.banking.constants.TransactionType;
import com.banking.entity.Account;
import com.banking.entity.AuditLog;
import com.banking.entity.Role;
import com.banking.entity.Transaction;
import com.banking.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.AuditLogRepository;
import com.banking.repository.RoleRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.util.AccountNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AuditLogRepository auditLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing Seed Data for Apex Online Banking System...");

        // 1. Roles Initializer
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CUSTOMER)));

        // 2. Admin Seed User: dev@bank.com / Dev@123
        if (!userRepository.existsByEmail("dev@bank.com")) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setEmail("dev@bank.com");
            admin.setPassword(passwordEncoder.encode("Dev@123"));
            admin.setPhone("+1-800-555-0100");
            admin.setAddress("Apex Bank HQ, Financial District");
            admin.setEnabled(true);
            admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
            userRepository.save(admin);
            logger.info("Created Default Admin User: dev@bank.com / Dev@123");
        }

        // 3. Customer 1 Seed User: basu@bank.com / Basu@123
        if (!userRepository.existsByEmail("basu@bank.com")) {
            User basu = new User();
            basu.setFirstName("Basu");
            basu.setLastName("Dev");
            basu.setEmail("basu@bank.com");
            basu.setPassword(passwordEncoder.encode("Basu@123"));
            basu.setPhone("+1-800-555-0101");
            basu.setAddress("123 Innovation Way, Suite 400");
            basu.setEnabled(true);
            basu.setRoles(new HashSet<>(Collections.singletonList(customerRole)));
            User savedBasu = userRepository.save(basu);

            Account accountBasu = new Account();
            accountBasu.setAccountNumber("100020003001");
            accountBasu.setBalance(new BigDecimal("50000.00"));
            accountBasu.setAccountType(AccountType.SAVINGS);
            accountBasu.setAccountStatus(AccountStatus.ACTIVE);
            accountBasu.setBranch("Main Financial District Branch");
            accountBasu.setIfscCode("APEX000101");
            accountBasu.setUser(savedBasu);
            Account savedAccBasu = accountRepository.save(accountBasu);

            Transaction txnBasu = new Transaction();
            txnBasu.setTransactionId(AccountNumberGenerator.generateTransactionId());
            txnBasu.setReceiverAccount(savedAccBasu);
            txnBasu.setAmount(new BigDecimal("50000.00"));
            txnBasu.setTransactionType(TransactionType.DEPOSIT);
            txnBasu.setStatus(TransactionStatus.SUCCESS);
            txnBasu.setRemarks("Initial Account Opening Deposit");
            transactionRepository.save(txnBasu);

            logger.info("Created Default Customer 1: basu@bank.com / Basu@123 (Acc: 100020003001)");
        }

        // 4. Customer 2 Seed User: bharath@bank.com / Bharath@123
        if (!userRepository.existsByEmail("bharath@bank.com")) {
            User bharath = new User();
            bharath.setFirstName("Bharath");
            bharath.setLastName("Kumar");
            bharath.setEmail("bharath@bank.com");
            bharath.setPassword(passwordEncoder.encode("Bharath@123"));
            bharath.setPhone("+1-800-555-0102");
            bharath.setAddress("456 Tech Boulevard, Floor 12");
            bharath.setEnabled(true);
            bharath.setRoles(new HashSet<>(Collections.singletonList(customerRole)));
            User savedBharath = userRepository.save(bharath);

            Account accountBharath = new Account();
            accountBharath.setAccountNumber("100020003002");
            accountBharath.setBalance(new BigDecimal("75000.50"));
            accountBharath.setAccountType(AccountType.SAVINGS);
            accountBharath.setAccountStatus(AccountStatus.ACTIVE);
            accountBharath.setBranch("Main Financial District Branch");
            accountBharath.setIfscCode("APEX000101");
            accountBharath.setUser(savedBharath);
            Account savedAccBharath = accountRepository.save(accountBharath);

            Transaction txnBharath = new Transaction();
            txnBharath.setTransactionId(AccountNumberGenerator.generateTransactionId());
            txnBharath.setReceiverAccount(savedAccBharath);
            txnBharath.setAmount(new BigDecimal("75000.50"));
            txnBharath.setTransactionType(TransactionType.DEPOSIT);
            txnBharath.setStatus(TransactionStatus.SUCCESS);
            txnBharath.setRemarks("Initial Account Opening Deposit");
            transactionRepository.save(txnBharath);

            logger.info("Created Default Customer 2: bharath@bank.com / Bharath@123 (Acc: 100020003002)");
        }

        // 5. Initial System Audit Log
        if (auditLogRepository.count() == 0) {
            AuditLog auditLog = new AuditLog("SYSTEM", "INITIALIZATION",
                    "Apex Online Banking System database initialized with default roles and accounts.", "127.0.0.1");
            auditLogRepository.save(auditLog);
        }

        logger.info("Seed Data Initialization Completed Successfully!");
    }
}
