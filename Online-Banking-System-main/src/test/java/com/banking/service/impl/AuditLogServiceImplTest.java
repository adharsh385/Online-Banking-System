package com.banking.service.impl;

import com.banking.entity.AuditLog;
import com.banking.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void recordsAuditActionAndUsesLoopbackForMissingIp() {
        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository);

        service.logAction("customer@example.test", "LOGIN", "User signed in", null);

        ArgumentCaptor<AuditLog> log = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(log.capture());
        assertEquals("customer@example.test", log.getValue().getUserEmail());
        assertEquals("LOGIN", log.getValue().getAction());
        assertEquals("User signed in", log.getValue().getDetails());
        assertEquals("127.0.0.1", log.getValue().getIpAddress());
        assertNotNull(log.getValue().getTimestamp());
    }

    @Test
    void returnsRecentAuditLogsFromRepository() {
        List<AuditLog> logs = List.of(new AuditLog("admin@example.test", "LOGIN", "Signed in", "127.0.0.1"));
        when(auditLogRepository.findTop50ByOrderByTimestampDesc()).thenReturn(logs);

        assertSame(logs, new AuditLogServiceImpl(auditLogRepository).getRecentAuditLogs());
    }
}
