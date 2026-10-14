package lk.booknplay.service.impl;

import lk.booknplay.service.PayoutSettlementService;
import lk.booknplay.util.PayoutPeriodCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PayoutSettlementJob {

    private static final Logger log = LoggerFactory.getLogger(PayoutSettlementJob.class);

    private final PayoutSettlementService payoutSettlementService;

    public PayoutSettlementJob(PayoutSettlementService payoutSettlementService) {
        this.payoutSettlementService = payoutSettlementService;
    }

    /** Daily at 01:15 Asia/Colombo — generate settlements for completed weekly/monthly windows. */
    @Scheduled(cron = "${booknplay.payout.generate-cron:0 15 1 * * *}", zone = "Asia/Colombo")
    public void generateDaily() {
        LocalDate asOf = LocalDate.now(PayoutPeriodCalculator.ZONE);
        int created = payoutSettlementService.generateSettlements(asOf);
        if (created > 0) {
            log.info("Generated {} payout settlement(s) for asOf={}", created, asOf);
        }
    }
}
