package lk.booknplay.service.impl;

import lk.booknplay.service.OwnerSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks past-due trials/subscriptions as EXPIRED and archives their live venues
 * so they leave customer homepage / public discovery without requiring owner login.
 */
@Component
public class SubscriptionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryJob.class);

    private final OwnerSubscriptionService ownerSubscriptionService;

    public SubscriptionExpiryJob(OwnerSubscriptionService ownerSubscriptionService) {
        this.ownerSubscriptionService = ownerSubscriptionService;
    }

    @Scheduled(cron = "${booknplay.subscriptions.expiry-cron:0 0 * * * *}", zone = "Asia/Colombo")
    public void expireSubscriptionsAndArchiveVenues() {
        int expired = ownerSubscriptionService.expireDueSubscriptionsAndArchiveVenues();
        if (expired > 0) {
            log.info("Expired {} subscription(s) and archived their live venues", expired);
        }
    }
}
