package lk.booknplay.service;

import lk.booknplay.dto.request.AdminMarketingRequest;
import lk.booknplay.dto.response.AdminMarketingResponse;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.User;
import lk.booknplay.enums.CustomerStatus;
import lk.booknplay.enums.MarketingChannel;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.AdminAuditLogRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.service.impl.AdminMarketingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMarketingServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private SmsLenzService smsLenzService;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private JavaMailSender mailSender;
    @Mock private AdminAuditLogRepository auditLogs;

    private AdminMarketingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminMarketingServiceImpl(
                customerRepository, smsLenzService, mailSenderProvider, auditLogs);
    }

    private void stubAuditSave() {
        when(auditLogs.save(any(AdminAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void sendSms_SkipsMissingPhone_AndCountsSent() {
        stubAuditSave();
        Customer withPhone = customer("c1", "+94771111111", "a@example.com");
        Customer noPhone = customer("c2", null, "b@example.com");
        when(customerRepository.findAllByStatusWithUser(CustomerStatus.ACTIVE))
                .thenReturn(List.of(withPhone, noPhone));

        AdminMarketingResponse response = service.sendMarketing(
                AdminMarketingRequest.builder()
                        .channel(MarketingChannel.SMS)
                        .message("Hello players")
                        .build(),
                "admin@booknplay.lk");

        assertEquals(2, response.getTargeted());
        assertEquals(1, response.getSent());
        assertEquals(1, response.getSkipped());
        assertEquals(0, response.getFailed());
        verify(smsLenzService).sendSms("+94771111111", "Hello players");
        verify(smsLenzService, never()).sendSms(eq(null), anyString());

        ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogs).save(auditCaptor.capture());
        assertEquals("MARKETING_SMS", auditCaptor.getValue().getAction());
        assertEquals("CUSTOMER", auditCaptor.getValue().getResourceType());
    }

    @Test
    void sendEmail_SkipsMissingEmail_AndCountsFailures() {
        stubAuditSave();
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        Customer ok = customer("c1", "+94771111111", "ok@example.com");
        Customer blank = customer("c2", "+94772222222", "  ");
        Customer boom = customer("c3", "+94773333333", "fail@example.com");
        when(customerRepository.findAllByStatusWithUser(CustomerStatus.ACTIVE))
                .thenReturn(List.of(ok, blank, boom));
        org.mockito.Mockito.doAnswer(inv -> {
            SimpleMailMessage msg = inv.getArgument(0);
            if ("fail@example.com".equals(msg.getTo()[0])) {
                throw new RuntimeException("smtp down");
            }
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        AdminMarketingResponse response = service.sendMarketing(
                AdminMarketingRequest.builder()
                        .channel(MarketingChannel.EMAIL)
                        .subject("Promo")
                        .message("Book this weekend")
                        .build(),
                "admin@booknplay.lk");

        assertEquals(3, response.getTargeted());
        assertEquals(1, response.getSent());
        assertEquals(1, response.getSkipped());
        assertEquals(1, response.getFailed());
        assertEquals(1, response.getFailureSamples().size());
        assertTrue(response.getFailureSamples().get(0).contains("fail@example.com"));
        verify(auditLogs).save(any(AdminAuditLog.class));
    }

    @Test
    void sendBoth_RequiresSubject_AndSendsBothChannels() {
        stubAuditSave();
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        Customer c = customer("c1", "+94771111111", "a@example.com");
        when(customerRepository.findAllByStatusAndIdInWithUser(eq(CustomerStatus.ACTIVE), any()))
                .thenReturn(List.of(c));

        assertThrows(BadRequestException.class, () -> service.sendMarketing(
                AdminMarketingRequest.builder()
                        .channel(MarketingChannel.BOTH)
                        .message("Hi")
                        .customerIds(List.of("c1"))
                        .build(),
                "admin@booknplay.lk"));

        AdminMarketingResponse response = service.sendMarketing(
                AdminMarketingRequest.builder()
                        .channel(MarketingChannel.BOTH)
                        .subject("Deal")
                        .message("Hi")
                        .customerIds(List.of("c1"))
                        .build(),
                "admin@booknplay.lk");

        assertEquals(1, response.getTargeted());
        assertEquals(1, response.getSent());
        verify(smsLenzService).sendSms("+94771111111", "Hi");
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(auditLogs, times(2)).save(any(AdminAuditLog.class));
    }

    @Test
    void exportCustomersCsv_ExcludesDeleted_AndEscapesFields() {
        Customer active = customer("c1", "+94771111111", "a@example.com");
        active.setFirstName("Ann, \"Ace\"");
        when(customerRepository.findAllExcludingStatusWithUser(CustomerStatus.DELETED))
                .thenReturn(List.of(active));

        String csv = service.exportCustomersCsv();

        assertTrue(csv.startsWith("id,firstName,lastName,email,phone,status,createdAt\n"));
        assertTrue(csv.contains("\"Ann, \"\"Ace\"\"\""));
        assertTrue(csv.contains("a@example.com"));
        assertTrue(csv.contains("ACTIVE"));
    }

    private static Customer customer(String id, String phone, String email) {
        User user = User.builder().email(email).build();
        return Customer.builder()
                .id(id)
                .firstName("First")
                .lastName("Last")
                .phone(phone)
                .status(CustomerStatus.ACTIVE)
                .user(user)
                .build();
    }
}
