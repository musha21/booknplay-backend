-- BooknPlay baseline schema for MySQL 8.4
-- Flyway V1: creates empty production-compatible schema for Hibernate ddl-auto=validate.
-- Includes generated columns/indexes from BookingSlotUniqueSchemaFix and SportsCatalogMigrationRunner.
-- No reference/business data (CatalogDataSeeder / SubscriptionPlanSeeder remain authoritative).
-- Do NOT apply to a database that already has Hibernate-created tables without flyway baseline.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `admin_audit_logs` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin_email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `resource_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `resource_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `blocked_slots` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `date` date NOT NULL,
  `end_time` time(6) NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_time` time(6) NOT NULL,
  `court_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4a1qyy8g9sjycmw79tb4ef630` (`court_id`),
  CONSTRAINT `FK4a1qyy8g9sjycmw79tb4ef630` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `booking_quotes` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `booking_date` date NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancellation_allowed` bit(1) NOT NULL,
  `cancellation_deadline` datetime(6) DEFAULT NULL,
  `consumed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `end_time` time(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `late_refund_percentage` decimal(5,2) NOT NULL,
  `promo_code` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `selected_slots` text COLLATE utf8mb4_unicode_ci,
  `start_time` time(6) NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `court_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sport_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_booking_quote_customer_expiry` (`customer_id`,`expires_at`),
  KEY `FKe69qc1lmon0h5bes2jg6s5jui` (`court_id`),
  KEY `FKtoqybclso3557feqgcpw6quuv` (`sport_id`),
  CONSTRAINT `FKe69qc1lmon0h5bes2jg6s5jui` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`),
  CONSTRAINT `FKmlkdeycmsx666i8u8oyjpjkg8` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKtoqybclso3557feqgcpw6quuv` FOREIGN KEY (`sport_id`) REFERENCES `sports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `booking_slots` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_time` time(6) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `start_time` time(6) NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_booking_slot_booking` (`booking_id`),
  KEY `idx_booking_slot_range` (`start_time`,`end_time`),
  CONSTRAINT `FKfbxpy4ovy5hh9e1f9vyolha2x` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `bookings` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `booking_date` date NOT NULL,
  `booking_ref` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cancellation_allowed` bit(1) DEFAULT NULL,
  `cancellation_deadline` datetime(6) DEFAULT NULL,
  `cancellation_fee` decimal(12,2) DEFAULT NULL,
  `cancellation_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `cancelled_by` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_time` time(6) NOT NULL,
  `guest_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `guest_phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `late_refund_percentage` decimal(5,2) DEFAULT NULL,
  `policy_snapshot_source` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refund_amount` decimal(12,2) DEFAULT NULL,
  `source` enum('ONLINE','WALK_IN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `special_requests` text COLLATE utf8mb4_unicode_ci,
  `start_time` time(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','FAILED','NO_SHOW','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_amount` decimal(38,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `court_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sport_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active_court_slot` varchar(120) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'PENDING',_utf8mb4'CONFIRMED')) then concat(`court_id`,_utf8mb4'|',`booking_date`,_utf8mb4'|',`start_time`,_utf8mb4'|',`end_time`) else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmixjojjfxsussm3x48inu5n7i` (`booking_ref`),
  UNIQUE KEY `uk_customer_idempotency` (`customer_id`,`idempotency_key`),
  UNIQUE KEY `uk_active_court_slot` (`active_court_slot`),
  KEY `FKrc2ar1jl63nymaipwibvab7q0` (`court_id`),
  KEY `FKfsj3akue05uu2di2baygsdpiw` (`sport_id`),
  KEY `FKoxvw27ybrq6bujd4kbobnmh1p` (`venue_id`),
  CONSTRAINT `FKbvfibgflhsb0g2hnjauiv5khs` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKfsj3akue05uu2di2baygsdpiw` FOREIGN KEY (`sport_id`) REFERENCES `sports` (`id`),
  CONSTRAINT `FKoxvw27ybrq6bujd4kbobnmh1p` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`),
  CONSTRAINT `FKrc2ar1jl63nymaipwibvab7q0` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `business_images` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_logo` bit(1) NOT NULL,
  `sort_order` int NOT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfnxms06heqv7b3l7ok6jc2oa3` (`business_id`),
  CONSTRAINT `FKfnxms06heqv7b3l7ok6jc2oa3` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `business_staff` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `can_billing` bit(1) NOT NULL,
  `can_calendar` bit(1) NOT NULL,
  `can_courts` bit(1) NOT NULL,
  `can_earnings` bit(1) NOT NULL,
  `can_reports` bit(1) NOT NULL,
  `can_venues` bit(1) NOT NULL,
  `can_walk_ins` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_staff_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `business_subscriptions` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cancel_at_period_end` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_period_end` datetime(6) DEFAULT NULL,
  `current_period_start` datetime(6) DEFAULT NULL,
  `plan_code` enum('GROWTH','PRO','STARTER','TRIAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','CANCELED','EXPIRED','PAST_DUE','TRIALING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `trial_ends_at` datetime(6) DEFAULT NULL,
  `trial_starts_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_business_subscriptions_business` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `businesses` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_branch` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_verification_status` enum('PENDING_REVIEW','UNVERIFIED','VERIFIED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `bank_verified_at` datetime(6) DEFAULT NULL,
  `bank_verified_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `commission_percent` decimal(5,2) NOT NULL,
  `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `logo_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `notify_booking_email` bit(1) NOT NULL,
  `notify_booking_sms` bit(1) NOT NULL,
  `notify_payment_email` bit(1) NOT NULL,
  `notify_trial_email` bit(1) NOT NULL,
  `owner_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_profile_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payout_frequency` enum('MONTHLY','WEEKLY') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `cancellation_policies` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hours_before_deadline` int NOT NULL,
  `refund_percentage` decimal(38,2) NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgo5hbinchsy7ev28c7gx5ac82` (`business_id`),
  CONSTRAINT `FKgo5hbinchsy7ev28c7gx5ac82` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `court_pricings` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `end_time` time(6) NOT NULL,
  `label` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) NOT NULL,
  `priority` int DEFAULT NULL,
  `rule_type` enum('CUSTOM','PEAK','WEEKEND') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_time` time(6) NOT NULL,
  `court_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKisagrv4j0tftgnw72cckwjdyn` (`court_id`),
  CONSTRAINT `FKisagrv4j0tftgnw72cckwjdyn` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `courts` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `duration_minutes` int NOT NULL,
  `facility_type` enum('INDOOR','OUTDOOR') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hourly_rate` decimal(38,2) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','DELETED','INACTIVE','UNDER_MAINTENANCE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `sport_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKl47nyo4h27u5jruxs7utlo0va` (`sport_id`),
  KEY `FKc588eogjvkwv2gfs9ulycwbk1` (`venue_id`),
  CONSTRAINT `FKc588eogjvkwv2gfs9ulycwbk1` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`),
  CONSTRAINT `FKl47nyo4h27u5jruxs7utlo0va` FOREIGN KEY (`sport_id`) REFERENCES `sports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `customers` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `first_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `profile_image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ACTIVE','DELETED','INACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm3iom37efaxd5eucmxjqqcbe9` (`phone`),
  UNIQUE KEY `UKeuat1oase6eqv195jvb71a93s` (`user_id`),
  CONSTRAINT `FKrh1g1a20omjmn6kurd35o3eit` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `homepage_configs` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `animation_intensity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `eyebrow` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `featured_business_ids` text COLLATE utf8mb4_unicode_ci,
  `featured_sport_ids` text COLLATE utf8mb4_unicode_ci,
  `featured_venue_ids` text COLLATE utf8mb4_unicode_ci,
  `heading` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `premium_business_ids` text COLLATE utf8mb4_unicode_ci,
  `premium_slider_arrows` bit(1) NOT NULL,
  `premium_slider_autoplay` bit(1) NOT NULL,
  `premium_slider_enabled` bit(1) NOT NULL,
  `premium_slider_indicators` bit(1) NOT NULL,
  `premium_slider_pause_on_hover` bit(1) NOT NULL,
  `premium_slider_seconds` int NOT NULL,
  `premium_slides` text COLLATE utf8mb4_unicode_ci,
  `published_at` datetime(6) DEFAULT NULL,
  `section_order` text COLLATE utf8mb4_unicode_ci,
  `show_businesses` bit(1) NOT NULL,
  `show_cities` bit(1) NOT NULL,
  `show_how_it_works` bit(1) NOT NULL,
  `show_owner_promotion` bit(1) NOT NULL,
  `show_search` bit(1) NOT NULL,
  `show_sports` bit(1) NOT NULL,
  `show_trust` bit(1) NOT NULL,
  `show_venues` bit(1) NOT NULL,
  `status` enum('ARCHIVED','DRAFT','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `invoices` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `invoice_number` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_amount` decimal(38,2) NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl1x55mfsay7co0r3m9ynvipd5` (`invoice_number`),
  UNIQUE KEY `UKqn380ix1ge287r0rd8th12bwi` (`booking_id`),
  KEY `FKq2w4hmh6l9othnp6cepp0cfe2` (`customer_id`),
  CONSTRAINT `FKb9bhb7xre5v64qvjeholh3qj0` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKq2w4hmh6l9othnp6cepp0cfe2` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `maintenance_windows` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `end_date_time` datetime(6) NOT NULL,
  `start_date_time` datetime(6) NOT NULL,
  `court_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKseyaxjsk6011vjm2nbltlb6fw` (`court_id`),
  CONSTRAINT `FKseyaxjsk6011vjm2nbltlb6fw` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `operating_hours` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `close_time` time(6) NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_closed` bit(1) NOT NULL,
  `open_time` time(6) NOT NULL,
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKld1aojx0t6k3y8a2gkp4dpty3` (`venue_id`),
  CONSTRAINT `FKld1aojx0t6k3y8a2gkp4dpty3` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `otp_verifications` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempt_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `otp_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purpose` enum('LOGIN','PHONE_VERIFICATION','REGISTER','RESET_PASSWORD') COLLATE utf8mb4_unicode_ci NOT NULL,
  `verified` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_otp_phone_purpose` (`phone`,`purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `payments` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(38,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gateway_reference` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_gateway` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('FAILED','INITIATED','PAID','PARTIALLY_REFUNDED','PROCESSING','REFUNDED','SUCCESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnuscjm6x127hkb15kcb8n56wo` (`booking_id`),
  KEY `FK45dp0030s8e3myd8n6ky4e79g` (`customer_id`),
  CONSTRAINT `FK45dp0030s8e3myd8n6ky4e79g` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKc52o2b1jkxttngufqp3t7jr3h` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `payout_items` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adjustment` bit(1) NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `commission` decimal(12,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `gateway_fee` decimal(12,2) NOT NULL,
  `gross` decimal(12,2) NOT NULL,
  `net` decimal(12,2) NOT NULL,
  `payment_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `refund` decimal(12,2) NOT NULL,
  `refund_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payout_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_payout_item_payment` (`payment_id`),
  KEY `idx_payout_item_payout` (`payout_id`),
  CONSTRAINT `FK5vows29lpnh53pbsvafjngear` FOREIGN KEY (`payout_id`) REFERENCES `payouts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `payouts` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `commission` decimal(12,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `frequency` enum('MONTHLY','WEEKLY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `gateway_fees` decimal(12,2) NOT NULL,
  `gross` decimal(12,2) NOT NULL,
  `net` decimal(12,2) NOT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `paid_by_admin_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `period_end` date NOT NULL,
  `period_start` date NOT NULL,
  `receipt_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refunds_total` decimal(12,2) NOT NULL,
  `status` enum('ON_HOLD','PAID','PENDING','PROCESSING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `transfer_amount` decimal(12,2) DEFAULT NULL,
  `transfer_date` date DEFAULT NULL,
  `transfer_reference` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payout_business_period` (`business_id`,`period_start`,`period_end`),
  CONSTRAINT `FK5wb7uwxmdyi5o1o7uflbqdbwu` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `platform_payout_config` (
  `id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gateway_fee_percent` decimal(5,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `promotions` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `max_redemptions` int DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `redemption_count` int NOT NULL,
  `sport_ids` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_date` date NOT NULL,
  `type` enum('FIXED','PERCENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` decimal(12,2) NOT NULL,
  `venue_ids` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_promotion_business_code` (`business_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `refresh_tokens` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `is_revoked` bit(1) NOT NULL,
  `token` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `refunds` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `processed_at` datetime(6) NOT NULL,
  `reference` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_booking` (`booking_id`),
  UNIQUE KEY `UK54tggwvh1uu62y25jggxwmjxg` (`reference`),
  UNIQUE KEY `UKl8mus5nda5anlgvjnu8dfg0i8` (`payment_id`),
  CONSTRAINT `FK3nlfu8o4nsl1kcfp2sg8q4gnd` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FKpt9ic0j1y6xwlej99wnynvnpy` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `reviews` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) NOT NULL,
  `rating` int NOT NULL,
  `booking_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3p9j9vyr1qofbcxju65es206r` (`booking_id`),
  KEY `FK4sm0k8kw740iyuex3vwwv1etu` (`customer_id`),
  KEY `FKj3q0yctu4l1j9m0vj1ddj9y3s` (`venue_id`),
  CONSTRAINT `FK28an517hrxtt2bsg93uefugrm` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `FK4sm0k8kw740iyuex3vwwv1etu` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `FKj3q0yctu4l1j9m0vj1ddj9y3s` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `sports` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_norm` varchar(255) COLLATE utf8mb4_unicode_ci GENERATED ALWAYS AS (lower(trim(`name`))) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtj61or3k005spbrx0lgjpwtde` (`name`),
  UNIQUE KEY `uk_sports_name_norm` (`name_norm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `subscription_payments` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `billing_interval` enum('MONTHLY','YEARLY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkout_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gateway_reference` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_gateway` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plan_code` enum('GROWTH','PRO','STARTER','TRIAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('FAILED','INITIATED','PAID','PARTIALLY_REFUNDED','PROCESSING','REFUNDED','SUCCESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4rbilaoqeqjxfb9knuo7kqtt6` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `subscription_plans` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` bit(1) NOT NULL,
  `advanced_reports_enabled` bit(1) NOT NULL,
  `calendar_enabled` bit(1) NOT NULL,
  `code` enum('GROWTH','PRO','STARTER','TRIAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `commission_percent` decimal(5,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `earnings_enabled` bit(1) NOT NULL,
  `features` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `highlighted` bit(1) NOT NULL,
  `max_courts_per_venue` int DEFAULT NULL,
  `max_staff` int DEFAULT NULL,
  `max_venues` int DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price_monthly` decimal(12,2) NOT NULL,
  `price_yearly` decimal(12,2) NOT NULL,
  `promotions_enabled` bit(1) NOT NULL,
  `reports_enabled` bit(1) NOT NULL,
  `sort_order` int NOT NULL,
  `trial_duration_days` int DEFAULT NULL,
  `walk_in_enabled` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK95fpyxycxc00w3yabipw8jrq7` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `users` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email_verified` bit(1) NOT NULL,
  `is_enabled` bit(1) NOT NULL,
  `is_locked` bit(1) NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone_verified` bit(1) NOT NULL,
  `role` enum('BUSINESS_OWNER','CUSTOMER','STAFF','SUPER_ADMIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKdu5v5sr43g5bfnji4vb8hg5s3` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `venue_amenities` (
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amenity` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  KEY `FK4wa7mhqnf0cesdsrxl7heh0gp` (`venue_id`),
  CONSTRAINT `FK4wa7mhqnf0cesdsrxl7heh0gp` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `venue_images` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKee7nuhndkpdm58nx1g7vdox9l` (`venue_id`),
  CONSTRAINT `FKee7nuhndkpdm58nx1g7vdox9l` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `venue_rules` (
  `venue_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rule_text` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  KEY `FKk5weq64c05mpaxp2i70fews5g` (`venue_id`),
  CONSTRAINT `FKk5weq64c05mpaxp2i70fews5g` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `venues` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `additional_rules` text COLLATE utf8mb4_unicode_ci,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `archived_from_status` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_image_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `formatted_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `venue_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjpsl32usba0or0v3o72t22gki` (`business_id`),
  CONSTRAINT `FKjpsl32usba0or0v3o72t22gki` FOREIGN KEY (`business_id`) REFERENCES `businesses` (`id`),
  CONSTRAINT `venues_chk_1` CHECK ((`archived_from_status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_APPROVAL',_utf8mb4'APPROVED',_utf8mb4'ACTIVE',_utf8mb4'REJECTED',_utf8mb4'SUSPENDED',_utf8mb4'INACTIVE',_utf8mb4'DELETED'))),
  CONSTRAINT `venues_chk_2` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_APPROVAL',_utf8mb4'APPROVED',_utf8mb4'ACTIVE',_utf8mb4'REJECTED',_utf8mb4'SUSPENDED',_utf8mb4'INACTIVE',_utf8mb4'DELETED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
