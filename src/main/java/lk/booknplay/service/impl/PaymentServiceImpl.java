package lk.booknplay.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lk.booknplay.dto.response.InvoiceResponse;
import lk.booknplay.dto.response.PaymentResponse;
import lk.booknplay.entity.Booking;
import lk.booknplay.entity.BookingSlot;
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Invoice;
import lk.booknplay.entity.Payment;
import lk.booknplay.enums.BookingSource;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.PaymentStatus;
import lk.booknplay.exception.BadRequestException;
import lk.booknplay.exception.ForbiddenException;
import lk.booknplay.exception.ResourceNotFoundException;
import lk.booknplay.exception.UnauthorizedException;
import lk.booknplay.repository.BookingRepository;
import lk.booknplay.repository.CustomerRepository;
import lk.booknplay.repository.InvoiceRepository;
import lk.booknplay.repository.PaymentRepository;
import lk.booknplay.service.NotificationService;
import lk.booknplay.service.OwnerSubscriptionService;
import lk.booknplay.service.PayHereCheckoutStore;
import lk.booknplay.service.PayHereRefundClient;
import lk.booknplay.service.PaymentService;
import lk.booknplay.service.impl.OwnerSubscriptionServiceImpl;
import lk.booknplay.util.PayHereHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final PayHereCheckoutStore checkoutStore;
    private final PayHereRefundClient payHereRefundClient;
    private final OwnerSubscriptionService ownerSubscriptionService;
    private final String paymentsMode;
    private final String appBaseUrl;
    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutUrl;
    private final String returnUrlBase;
    private final String cancelUrlBase;
    private final String notifyUrl;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            NotificationService notificationService,
            PayHereCheckoutStore checkoutStore,
            PayHereRefundClient payHereRefundClient,
            @Lazy OwnerSubscriptionService ownerSubscriptionService,
            @Value("${booknplay.payments.mode:PAYHERE}") String paymentsMode,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl,
            @Value("${payhere.merchant-id:}") String merchantId,
            @Value("${payhere.merchant-secret:}") String merchantSecret,
            @Value("${payhere.checkout-url:https://sandbox.payhere.lk/pay/checkout}") String checkoutUrl,
            @Value("${payhere.return-url:http://localhost:5173/payment/return}") String returnUrlBase,
            @Value("${payhere.cancel-url:http://localhost:5173/payment/return}") String cancelUrlBase,
            @Value("${payhere.notify-url:http://localhost:8080/api/v1/webhook/payhere/notify}") String notifyUrl) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationService = notificationService;
        this.checkoutStore = checkoutStore;
        this.payHereRefundClient = payHereRefundClient;
        this.ownerSubscriptionService = ownerSubscriptionService;
        this.paymentsMode = paymentsMode;
        this.appBaseUrl = trimTrailingSlash(appBaseUrl);
        this.merchantId = merchantId == null ? "" : merchantId.trim();
        this.merchantSecret = merchantSecret == null ? "" : merchantSecret.trim();
        this.checkoutUrl = checkoutUrl;
        this.returnUrlBase = returnUrlBase;
        this.cancelUrlBase = cancelUrlBase;
        this.notifyUrl = notifyUrl;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(String customerEmail, String bookingId, String gateway) {
        requirePayHereMode();
        if (gateway != null && !gateway.isBlank() && !"PAYHERE".equalsIgnoreCase(gateway.trim())) {
            throw new BadRequestException("Only PAYHERE sandbox payments are supported");
        }
        if (merchantId.isBlank() || merchantSecret.isBlank()) {
            log.error(
                    "PayHere initiate blocked: merchantIdPresent={}, secretConfigured={}, checkoutUrl={}",
                    !merchantId.isBlank(),
                    !merchantSecret.isBlank(),
                    checkoutUrl
            );
            throw new BadRequestException(
                    "PayHere merchant credentials are not configured. "
                            + "Add your checkout host (localhost or ngrok host) in PayHere sandbox Integrations, "
                            + "copy THAT domain's merchant secret into application-local.properties, "
                            + "set app.base-url to the same host, then restart the API."
            );
        }

        log.info(
                "PayHere initiate: merchantId={}, checkoutUrl={}, secretConfigured=true",
                merchantId,
                checkoutUrl
        );
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getSource() == BookingSource.WALK_IN) {
            throw new BadRequestException("Walk-in bookings are paid in cash and cannot use PayHere");
        }

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Payment can only be initiated for a pending booking");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.PAID) {
            return mapToResponse(payment, null);
        }

        String firstName = splitName(booking.getContactName(), true, customer.getFirstName());
        String lastName = splitName(booking.getContactName(), false, customer.getLastName());
        String email = booking.getContactEmail() != null ? booking.getContactEmail() : customer.getUser().getEmail();
        String phone = booking.getContactPhone() != null ? booking.getContactPhone() : customer.getPhone();
        String hash = PayHereHash.checkoutHash(merchantId, booking.getBookingRef(), payment.getAmount(), payment.getCurrency(), merchantSecret);
        log.info(
                "PayHere checkout fields: orderId={}, amount={}, currency={}, merchantId={}, hashLength={}",
                booking.getBookingRef(),
                PayHereHash.formatAmount(payment.getAmount()),
                payment.getCurrency(),
                merchantId,
                hash.length()
        );
        String token = PayHereCheckoutStore.newToken();
        checkoutStore.put(new PayHereCheckoutStore.Session(
                token,
                booking.getId(),
                booking.getBookingRef(),
                payment.getAmount(),
                payment.getCurrency(),
                firstName,
                lastName,
                email,
                phone == null ? "" : phone,
                booking.getVenue().getName() + " — " + booking.getCourt().getName(),
                hash
        ));

        payment.setPaymentGateway("PAYHERE");
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        return mapToResponse(payment, checkoutPageUrl(token));
    }

    @Override
    @Transactional
    public PaymentResponse processWebhook(String gateway, String bookingId, String gatewayReference, String status) {
        throw new BadRequestException("Use the PayHere notify endpoint for sandbox payment callbacks");
    }

    @Override
    @Transactional
    public PaymentResponse processPayHereNotify(Map<String, String> fields) {
        String orderId = firstNonBlank(fields.get("order_id"), fields.get("orderId"));
        if (orderId != null && orderId.startsWith(OwnerSubscriptionServiceImpl.ORDER_PREFIX)) {
            ownerSubscriptionService.confirmPayHereNotify(fields);
            return PaymentResponse.builder()
                    .id(orderId)
                    .status(PaymentStatus.SUCCESS)
                    .paymentGateway("PAYHERE")
                    .build();
        }

        requirePayHereMode();
        if (merchantSecret.isBlank()) {
            throw new BadRequestException("PayHere merchant secret is not configured");
        }

        String paymentId = firstNonBlank(fields.get("payment_id"), fields.get("paymentId"));
        String statusCode = firstNonBlank(fields.get("status_code"), fields.get("statusCode"));
        String md5sig = firstNonBlank(fields.get("md5sig"), fields.get("md5Sig"));
        String payhereAmount = firstNonBlank(fields.get("payhere_amount"), fields.get("payhereAmount"));
        String payhereCurrency = firstNonBlank(fields.get("payhere_currency"), fields.get("payhereCurrency"), "LKR");
        String merchantFromPayload = firstNonBlank(fields.get("merchant_id"), fields.get("merchantId"), merchantId);

        if (orderId == null || statusCode == null || md5sig == null || payhereAmount == null) {
            throw new BadRequestException("Incomplete PayHere notify payload");
        }

        String expected = PayHereHash.notifySignature(
                merchantFromPayload, orderId, payhereAmount, payhereCurrency, statusCode, merchantSecret);
        if (!expected.equalsIgnoreCase(md5sig)) {
            throw new BadRequestException("Invalid PayHere notify signature");
        }

        Booking booking = bookingRepository.findByBookingRef(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found for order " + orderId));
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.PAID) {
            return mapToResponse(payment, null);
        }

        if ("2".equals(statusCode.trim())) {
            if (paymentId == null || paymentId.isBlank() || !paymentId.trim().matches("\\d+")) {
                log.error(
                        "PayHere notify success for order {} but payment_id is missing or invalid (value={}). "
                                + "Refunds will not work until notify includes a numeric payment_id.",
                        orderId,
                        paymentId
                );
                throw new BadRequestException(
                        "PayHere notify succeeded but payment_id was missing. Refunds require a numeric payment_id."
                );
            }
            String numericPaymentId = paymentId.trim();
            if (hasCompetingHold(booking)) {
                failAndRefundConflict(booking, payment, numericPaymentId);
                return mapToResponse(payment, null);
            }
            confirmPaid(booking, payment, "PAYHERE", numericPaymentId);
        } else {
            payment.setPaymentGateway("PAYHERE");
            if (paymentId != null && paymentId.trim().matches("\\d+")) {
                payment.setGatewayReference(paymentId.trim());
            }
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.FAILED);
                bookingRepository.save(booking);
            }
        }
        return mapToResponse(payment, null);
    }

    @Override
    public PayHereCheckoutStore.Session requireCheckoutSession(String token) {
        return checkoutStore.get(token)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session expired or not found"));
    }

    @Override
    public String payHereCheckoutUrl() {
        return checkoutUrl;
    }

    @Override
    public String buildReturnUrl(String bookingId) {
        return UriComponentsBuilder.fromUriString(returnUrlBase)
                .replaceQueryParam("bookingId", bookingId)
                .build(true)
                .toUriString();
    }

    @Override
    public String buildCancelUrl(String bookingId) {
        return UriComponentsBuilder.fromUriString(cancelUrlBase)
                .replaceQueryParam("bookingId", bookingId)
                .build(true)
                .toUriString();
    }

    @Override
    public String payHereNotifyUrl() {
        return notifyUrl;
    }

    @Override
    public String payHereMerchantId() {
        return merchantId;
    }

    private void requirePayHereMode() {
        if (!"PAYHERE".equalsIgnoreCase(String.valueOf(paymentsMode).trim())) {
            throw new BadRequestException("Payments require PayHere sandbox mode");
        }
    }

    private String checkoutPageUrl(String token) {
        return appBaseUrl + "/api/v1/public/payhere/checkout/" + token;
    }

    private boolean hasCompetingHold(Booking booking) {
        String courtId = booking.getCourt().getId();
        if (booking.getSlots() != null && !booking.getSlots().isEmpty()) {
            for (BookingSlot slot : booking.getSlots()) {
                if (bookingRepository.existsOverlappingBookingExcluding(
                        courtId,
                        booking.getBookingDate(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        booking.getId())) {
                    return true;
                }
            }
            return false;
        }
        LocalTime start = booking.getStartTime();
        LocalTime end = booking.getEndTime();
        return bookingRepository.existsOverlappingBookingExcluding(
                courtId, booking.getBookingDate(), start, end, booking.getId());
    }

    private void failAndRefundConflict(Booking booking, Payment payment, String paymentId) {
        log.warn(
                "PayHere paid booking {} conflicts with another hold/confirmed booking; failing and refunding payment_id={}",
                booking.getBookingRef(),
                paymentId
        );
        payment.setPaymentGateway("PAYHERE");
        payment.setGatewayReference(paymentId);
        booking.setStatus(BookingStatus.FAILED);
        bookingRepository.save(booking);

        try {
            payHereRefundClient.refund(
                    paymentId,
                    payment.getAmount(),
                    payment.getAmount(),
                    "Booking " + booking.getBookingRef() + " slot conflict — automatic refund"
            );
            payment.setStatus(PaymentStatus.REFUNDED);
        } catch (RuntimeException ex) {
            log.error(
                    "Automatic conflict refund failed for booking {} payment_id={}: {}",
                    booking.getBookingRef(),
                    paymentId,
                    ex.getMessage()
            );
            payment.setStatus(PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);
    }

    private void confirmPaid(Booking booking, Payment payment, String gateway, String gatewayReference) {
        if (booking.getSource() == BookingSource.WALK_IN || booking.getCustomer() == null) {
            throw new BadRequestException("Walk-in bookings cannot be confirmed through PayHere");
        }

        payment.setPaymentGateway(gateway);
        payment.setGatewayReference(gatewayReference);
        payment.setStatus(PaymentStatus.SUCCESS);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        paymentRepository.save(payment);

        if (invoiceRepository.findByBookingId(booking.getId()).isEmpty()) {
            Invoice invoice = Invoice.builder()
                    .invoiceNumber("INV-" + LocalDateTime.now().getYear() + "-" + System.currentTimeMillis() % 1000000)
                    .booking(booking)
                    .customer(booking.getCustomer())
                    .issuedAt(LocalDateTime.now())
                    .totalAmount(booking.getTotalAmount())
                    .status("PAID")
                    .build();
            invoiceRepository.save(invoice);
        }

        notificationService.sendBookingNotification(booking.getCustomer(), booking, "BOOKING_CONFIRMED");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(String customerEmail, String bookingId) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (!payment.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied");
        }

        String paymentUrl = null;
        if (payment.getStatus() == PaymentStatus.PROCESSING || payment.getStatus() == PaymentStatus.INITIATED) {
            // Caller should re-initiate if they need a fresh checkout token; status polls do not mint URLs.
            paymentUrl = null;
        }
        return mapToResponse(payment, paymentUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String customerEmail, String bookingId) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not generated yet for this booking"));

        if (!invoice.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied");
        }

        Booking booking = invoice.getBooking();
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .bookingId(booking.getId())
                .bookingRef(booking.getBookingRef())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getUser().getEmail())
                .venueName(booking.getVenue().getName())
                .courtName(booking.getCourt().getName())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .issuedAt(invoice.getIssuedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(String customerEmail, String bookingId) {
        InvoiceResponse invoice = getInvoice(customerEmail, bookingId);
        Booking booking = bookingRepository.findByIdWithSlots(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph title = new Paragraph("BooknPlay.lk — Official Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), headerFont));
            document.add(new Paragraph("Issued At: " + invoice.getIssuedAt(), bodyFont));
            document.add(new Paragraph("Customer: " + invoice.getCustomerName() + " (" + invoice.getCustomerEmail() + ")", bodyFont));
            document.add(new Paragraph("Booking Ref: " + invoice.getBookingRef(), bodyFont));
            document.add(new Paragraph(" "));

            String sportName = booking.getSport() != null && booking.getSport().getName() != null
                    ? booking.getSport().getName()
                    : "Sport";
            String resourceLabel = lk.booknplay.util.SportCatalog.resourceLabel(sportName);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Venue");
            table.addCell(resourceLabel + " / Sport");
            table.addCell("Date & Time");
            table.addCell("Amount (LKR)");

            table.addCell(invoice.getVenueName());
            table.addCell(invoice.getCourtName() + " / " + sportName);
            table.addCell(booking.getBookingDate() + " (" + lk.booknplay.util.BookingTimeFormat.formatBooking(booking) + ")");
            table.addCell(invoice.getTotalAmount().toString());

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Paid: LKR " + invoice.getTotalAmount(), titleFont));
            document.add(new Paragraph("Status: " + invoice.getStatus(), headerFont));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate PDF invoice: " + e.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment payment, String paymentUrl) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .bookingRef(payment.getBooking().getBookingRef())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentGateway(payment.getPaymentGateway())
                .gatewayReference(payment.getGatewayReference())
                .status(payment.getStatus())
                .paymentUrl(paymentUrl)
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private static String splitName(String fullName, boolean first, String fallback) {
        if (fullName == null || fullName.isBlank()) {
            return fallback == null ? (first ? "Customer" : "Player") : fallback;
        }
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return first ? trimmed : "Player";
        }
        return first ? trimmed.substring(0, space) : trimmed.substring(space + 1).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "http://localhost:8080";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
