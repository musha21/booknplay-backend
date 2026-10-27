package lk.booknplay.service.impl;

import lk.booknplay.dto.request.AdminMarketingRequest;
import lk.booknplay.dto.response.AdminMarketingResponse;
import lk.booknplay.entity.AdminAuditLog;
import lk.booknplay.entity.Customer;
import lk.booknplay.enums.CustomerStatus;
import lk.booknplay.enums.MarketingChannel;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.repository.AdminAuditLogRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.service.AdminMarketingService;
import lk.booknplay.service.SmsLenzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AdminMarketingServiceImpl implements AdminMarketingService {

    private static final int MAX_FAILURE_SAMPLES = 5;

    private final CustomerRepository customerRepository;
    private final SmsLenzService smsLenzService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AdminAuditLogRepository auditLogs;

    public AdminMarketingServiceImpl(
            CustomerRepository customerRepository,
            SmsLenzService smsLenzService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            AdminAuditLogRepository auditLogs) {
        this.customerRepository = customerRepository;
        this.smsLenzService = smsLenzService;
        this.mailSenderProvider = mailSenderProvider;
        this.auditLogs = auditLogs;
    }

    @Override
    @Transactional
    public AdminMarketingResponse sendMarketing(AdminMarketingRequest request, String adminEmail) {
        MarketingChannel channel = request.getChannel();
        if (channel == null) {
            throw new BadRequestException("channel is required");
        }

        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isBlank()) {
            throw new BadRequestException("message is required");
        }

        boolean sendEmail = channel == MarketingChannel.EMAIL || channel == MarketingChannel.BOTH;
        boolean sendSms = channel == MarketingChannel.SMS || channel == MarketingChannel.BOTH;

        String subject = request.getSubject() == null ? "" : request.getSubject().trim();
        if (sendEmail && subject.isBlank()) {
            throw new BadRequestException("subject is required for email marketing");
        }

        List<Customer> recipients = loadRecipients(request.getCustomerIds());
        int targeted = recipients.size();
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        List<String> failureSamples = new ArrayList<>();

        JavaMailSender mailSender = sendEmail ? mailSenderProvider.getIfAvailable() : null;
        if (sendEmail && mailSender == null) {
            log.warn("JavaMailSender is not configured; email marketing will skip all recipients");
        }

        for (Customer customer : recipients) {
            boolean anySuccess = false;
            boolean anyFailure = false;
            boolean anyAttempt = false;

            if (sendSms) {
                String phone = customer.getPhone();
                if (phone != null && !phone.isBlank()) {
                    anyAttempt = true;
                    try {
                        smsLenzService.sendSms(phone, message);
                        anySuccess = true;
                    } catch (Exception ex) {
                        anyFailure = true;
                        addFailureSample(failureSamples, "SMS " + phone + ": " + ex.getMessage());
                        log.warn("Marketing SMS failed for customer {}: {}", customer.getId(), ex.getMessage());
                    }
                }
            }

            if (sendEmail) {
                String email = customer.getUser() != null ? customer.getUser().getEmail() : null;
                if (email != null && !email.isBlank() && mailSender != null) {
                    anyAttempt = true;
                    try {
                        SimpleMailMessage mail = new SimpleMailMessage();
                        mail.setTo(email);
                        mail.setSubject(subject);
                        mail.setText(message);
                        mailSender.send(mail);
                        anySuccess = true;
                    } catch (Exception ex) {
                        anyFailure = true;
                        addFailureSample(failureSamples, "EMAIL " + email + ": " + ex.getMessage());
                        log.warn("Marketing email failed for customer {}: {}", customer.getId(), ex.getMessage());
                    }
                }
            }

            if (anySuccess) {
                sent++;
            } else if (anyFailure) {
                failed++;
            } else if (!anyAttempt) {
                skipped++;
            }
        }

        String snippet = message.length() > 80 ? message.substring(0, 80) + "…" : message;
        String reason = "channel=" + channel
                + " targeted=" + targeted
                + " sent=" + sent
                + " skipped=" + skipped
                + " failed=" + failed
                + (sendEmail ? " subject=" + subject : "")
                + " message=" + snippet;

        if (sendSms) {
            audit(adminEmail, "MARKETING_SMS", reason);
        }
        if (sendEmail) {
            audit(adminEmail, "MARKETING_EMAIL", reason);
        }

        return AdminMarketingResponse.builder()
                .channel(channel)
                .targeted(targeted)
                .sent(sent)
                .skipped(skipped)
                .failed(failed)
                .failureSamples(failureSamples)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCustomersCsv() {
        List<Customer> customers = customerRepository.findAllExcludingStatusWithUser(CustomerStatus.DELETED);
        StringBuilder csv = new StringBuilder();
        csv.append("id,firstName,lastName,email,phone,status,createdAt\n");
        for (Customer c : customers) {
            String email = c.getUser() != null && c.getUser().getEmail() != null ? c.getUser().getEmail() : "";
            csv.append(csvCell(c.getId())).append(',')
                    .append(csvCell(c.getFirstName())).append(',')
                    .append(csvCell(c.getLastName())).append(',')
                    .append(csvCell(email)).append(',')
                    .append(csvCell(c.getPhone())).append(',')
                    .append(csvCell(c.getStatus() != null ? c.getStatus().name() : "")).append(',')
                    .append(csvCell(c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""))
                    .append('\n');
        }
        return csv.toString();
    }

    private List<Customer> loadRecipients(List<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return customerRepository.findAllByStatusWithUser(CustomerStatus.ACTIVE);
        }
        List<String> ids = customerIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return customerRepository.findAllByStatusWithUser(CustomerStatus.ACTIVE);
        }
        return customerRepository.findAllByStatusAndIdInWithUser(CustomerStatus.ACTIVE, ids);
    }

    private void audit(String adminEmail, String action, String reason) {
        auditLogs.save(AdminAuditLog.builder()
                .adminEmail(adminEmail != null ? adminEmail : "unknown")
                .action(action)
                .resourceType("CUSTOMER")
                .resourceId(null)
                .reason(reason)
                .build());
    }

    private static void addFailureSample(List<String> samples, String sample) {
        if (samples.size() < MAX_FAILURE_SAMPLES) {
            samples.add(sample);
        }
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
