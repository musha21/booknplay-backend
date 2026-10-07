package lk.booknplay.config;

import lk.booknplay.entity.Customer;
import lk.booknplay.entity.User;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.UserRepository;
import lk.booknplay.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time-ish backfill: copy normalized {@code customers.phone} onto {@code users.phone}
 * when the user phone is still null. Does not mark phones as verified.
 */
@Component
public class UserPhoneBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserPhoneBackfillRunner.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public UserPhoneBackfillRunner(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Customer> customers = customerRepository.findAll();
        int updated = 0;
        int skipped = 0;

        for (Customer customer : customers) {
            User user = customer.getUser();
            if (user == null || user.getPhone() != null) {
                continue;
            }
            String raw = customer.getPhone();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                String normalized = PhoneNumberUtil.normalizeSriLanka(raw);
                if (userRepository.existsByPhone(normalized)) {
                    skipped++;
                    continue;
                }
                user.setPhone(normalized);
                user.setPhoneVerified(false);
                userRepository.save(user);
                if (!normalized.equals(raw)) {
                    customer.setPhone(normalized);
                    customerRepository.save(customer);
                }
                updated++;
            } catch (Exception ex) {
                skipped++;
                log.debug("Skipping phone backfill for customer {}: {}", customer.getId(), ex.getMessage());
            }
        }

        if (updated > 0 || skipped > 0) {
            log.info("User phone backfill complete: updated={}, skipped={}", updated, skipped);
        }
    }
}
