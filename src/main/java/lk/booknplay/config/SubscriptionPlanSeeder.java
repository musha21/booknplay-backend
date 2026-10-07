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

@Component
@RequiredArgsConstructor
public class SubscriptionPlanSeeder implements ApplicationRunner {

    private final SubscriptionPlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsert(PlanCode.TRIAL, "Free trial",
                "90-day free trial with Growth-level capacity and 0% platform commission.",
                "0", "0", false, 0, false,
                5, null, 3, true, true, true, true, true, true,
                "0",
                "Up to 5 venues,3 staff seats,Unlimited courts,Calendar and walk-ins,Reports,Promotions,0% commission");

        upsert(PlanCode.STARTER, "Starter",
                "One venue workspace for independent courts and studios.",
                "4990", "49900", false, 1, true,
                1, null, 1, true, true, true, false, false, false,
                "10",
                "1 venue,1 staff seat,Unlimited courts,Calendar and walk-ins,Earnings overview,10% commission");

        upsert(PlanCode.GROWTH, "Growth",
                "Multi-venue operators who need reports and more capacity.",
                "9990", "99900", true, 2, true,
                5, null, 5, true, true, true, true, false, true,
                "10",
                "Up to 5 venues,5 staff seats,Reports,Promotions,Priority support,Everything in Starter,10% commission");

        upsert(PlanCode.PRO, "Pro",
                "Larger businesses that need scale and dedicated support.",
                "19990", "199900", false, 3, true,
                null, null, null, true, true, true, true, true, true,
                "10",
                "Unlimited venues,Unlimited staff,Advanced reporting,Promotions,Dedicated onboarding,Everything in Growth,10% commission");
    }

    private void upsert(
            PlanCode code, String name, String description,
            String monthly, String yearly, boolean highlighted, int sortOrder, boolean active,
            Integer maxVenues, Integer maxCourtsPerVenue, Integer maxStaff,
            boolean calendar, boolean walkIn, boolean earnings, boolean reports, boolean advancedReports,
            boolean promotions,
            String commissionPercent,
            String features) {
        SubscriptionPlan plan = planRepository.findByCode(code).orElseGet(() -> SubscriptionPlan.builder().code(code).build());
        plan.setName(name);
        plan.setDescription(description);
        plan.setPriceMonthly(new BigDecimal(monthly));
        plan.setPriceYearly(new BigDecimal(yearly));
        plan.setCurrency("LKR");
        plan.setHighlighted(highlighted);
        plan.setSortOrder(sortOrder);
        plan.setFeatures(features);
        plan.setActive(active);
        plan.setMaxVenues(maxVenues);
        plan.setMaxCourtsPerVenue(maxCourtsPerVenue);
        plan.setMaxStaff(maxStaff);
        plan.setCalendarEnabled(calendar);
        plan.setWalkInEnabled(walkIn);
        plan.setEarningsEnabled(earnings);
        plan.setReportsEnabled(reports);
        plan.setAdvancedReportsEnabled(advancedReports);
        plan.setPromotionsEnabled(promotions);
        plan.setCommissionPercent(new BigDecimal(commissionPercent));
        planRepository.save(plan);
    }
}
