package lk.booknplay.controller.publicapi;

import lk.booknplay.entity.Booking;
import lk.booknplay.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * PayHere often redirects (or POSTs) customers back to {@code return_url}/{@code cancel_url}.
 * Bridging through the API avoids Vite rejecting POSTs and recovers {@code bookingId}
 * from PayHere {@code order_id} when query params are dropped.
 */
@RestController
@RequestMapping("/api/v1/public/payhere")
@RequiredArgsConstructor
public class PayHereReturnController {

    private final BookingRepository bookingRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @RequestMapping(value = "/return", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView returnFromPayHere(@RequestParam Map<String, String> params) {
        return redirectToFrontend(params);
    }

    @RequestMapping(value = "/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView cancelFromPayHere(@RequestParam Map<String, String> params) {
        return redirectToFrontend(params);
    }

    private RedirectView redirectToFrontend(Map<String, String> params) {
        String bookingId = resolveBookingId(params);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(trimTrailingSlash(frontendUrl) + "/payment/return");
        if (StringUtils.hasText(bookingId)) {
            builder.replaceQueryParam("bookingId", bookingId);
        }
        String orderId = firstNonBlank(params.get("order_id"), params.get("orderId"), params.get("reference"));
        if (StringUtils.hasText(orderId)) {
            builder.replaceQueryParam("order_id", orderId);
            builder.replaceQueryParam("reference", orderId);
        }
        RedirectView view = new RedirectView(builder.build(true).toUriString());
        view.setStatusCode(HttpStatus.FOUND);
        view.setPropagateQueryParams(false);
        return view;
    }

    private String resolveBookingId(Map<String, String> params) {
        String bookingId = firstNonBlank(params.get("bookingId"), params.get("booking_id"));
        if (StringUtils.hasText(bookingId) && bookingRepository.existsById(bookingId.trim())) {
            return bookingId.trim();
        }
        String orderId = firstNonBlank(params.get("order_id"), params.get("orderId"), params.get("reference"));
        if (!StringUtils.hasText(orderId)) {
            return StringUtils.hasText(bookingId) ? bookingId.trim() : null;
        }
        Optional<Booking> byRef = bookingRepository.findByBookingRef(orderId.trim());
        return byRef.map(Booking::getId).orElse(StringUtils.hasText(bookingId) ? bookingId.trim() : null);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return null;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:5173";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
