package lk.booknplay.config;

import lk.booknplay.entity.SubscriptionPlan;
import lk.booknplay.enums.PlanCode;
import lk.booknplay.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds default subscription catalog rows once.
 * Existing rows are left untouched so Admin Plans edits survive restarts.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPlanSeeder implements ApplicationRunner {

    private final SubscriptionPlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        insertIfMissing(PlanCode.TRIAL, "Free trial",
                "Free trial with Growth-level capacity and 0% platform commission.",
                "0", "0", false, 0, false,
                5, null, 3, true, true, true, true, true, true,
                "0",
                "Up to 5 venues,3 staff seats,Unlimited courts,Calendar and walk-ins,Reports,Promotions,0% commission",
                90);

        insertIfMissing(PlanCode.STARTER, "Starter",
                "One venue workspace for independent courts and studios.",
                "4990", "49900", false, 1, true,
                1, null, 1, true, true, true, false, false, false,
                "10",
                "1 venue,1 staff seat,Unlimited courts,Calendar and walk-ins,Earnings overview,10% commission",
                null);

        insertIfMissing(PlanCode.GROWTH, "Growth",
                "Multi-venue operators who need reports and more capacity.",
                "9990", "99900", true, 2, true,
                5, null, 5, true, true, true, true, false, true,
                "10",
                "Up to 5 venues,5 staff seats,Reports,Promotions,Priority support,Everything in Starter,10% commission",
                null);

        insertIfMissing(PlanCode.PRO, "Pro",
                "Larger businesses that need scale and dedicated support.",
                "19990", "199900", false, 3, true,
                null, null, null, true, true, true, true, true, true,
                "10",
                "Unlimited venues,Unlimited staff,Advanced reporting,Promotions,Dedicated onboarding,Everything in Growth,10% commission",
                null);

        backfillNullTrialDurationDays();
    }

    /**
     * Legacy TRIAL rows may have null trial_duration_days after the column was added.
     * Fill the platform default once; never overwrite an admin-set value.
     */
    private void backfillNullTrialDurationDays() {
        planRepository.findByCode(PlanCode.TRIAL).ifPresent(plan -> {
            if (plan.getTrialDurationDays() == null) {
                plan.setTrialDurationDays(90);
                planRepository.save(plan);
            }
        });
    }

    private void insertIfMissing(
            PlanCode code, String name, String description,
            String monthly, String yearly, boolean highlighted, int sortOrder, boolean active,
            Integer maxVenues, Integer maxCourtsPerVenue, Integer maxStaff,
            boolean calendar, boolean walkIn, boolean earnings, boolean reports, boolean advancedReports,
            boolean promotions,
            String commissionPercent,
            String features,
            Integer trialDurationDays) {
        if (planRepository.findByCode(code).isPresent()) {
            return;
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code(code)
                .name(name)
                .description(description)
                .priceMonthly(new BigDecimal(monthly))
                .priceYearly(new BigDecimal(yearly))
                .currency("LKR")
                .highlighted(highlighted)
                .sortOrder(sortOrder)
                .features(features)
                .active(active)
                .maxVenues(maxVenues)
                .maxCourtsPerVenue(maxCourtsPerVenue)
                .maxStaff(maxStaff)
                .calendarEnabled(calendar)
                .walkInEnabled(walkIn)
                .earningsEnabled(earnings)
                .reportsEnabled(reports)
                .advancedReportsEnabled(advancedReports)
                .promotionsEnabled(promotions)
                .commissionPercent(new BigDecimal(commissionPercent))
                .trialDurationDays(trialDurationDays)
                .build();
        planRepository.save(plan);
    }
}
