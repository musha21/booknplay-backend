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
import lk.booknplay.entity.Customer;
import lk.booknplay.entity.Invoice;
import lk.booknplay.entity.Payment;
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
import lk.booknplay.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final String paymentsMode;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            NotificationService notificationService,
            @Value("${booknplay.payments.mode:DUMMY}") String paymentsMode) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.notificationService = notificationService;
        this.paymentsMode = paymentsMode;
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(String customerEmail, String bookingId, String gateway) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .customer(customer)
                        .amount(booking.getTotalAmount())
                        .currency("LKR")
                        .status(PaymentStatus.INITIATED)
                        .build());

        if (dummyMode()) {
            confirmPaid(booking, payment, "DUMMY", "DEV-" + booking.getBookingRef());
            return mapToResponse(payment, null);
        }

        String resolvedGateway = gateway == null || gateway.isBlank() ? "PAYHERE" : gateway.trim().toUpperCase();
        payment.setPaymentGateway(resolvedGateway);
        payment.setStatus(PaymentStatus.PROCESSING);
        Payment saved = paymentRepository.save(payment);
        String paymentUrl = "https://sandbox.payhere.lk/pay/checkout?order_id=" + booking.getBookingRef();
        return mapToResponse(saved, paymentUrl);
    }

    @Override
    @Transactional
    public PaymentResponse processWebhook(String gateway, String bookingId, String gatewayReference, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if ("SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            confirmPaid(booking, payment, gateway, gatewayReference);
        } else {
            payment.setPaymentGateway(gateway);
            payment.setGatewayReference(gatewayReference);
            payment.setStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.FAILED);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
        }

        return mapToResponse(payment, null);
    }

    private boolean dummyMode() {
        return paymentsMode == null || paymentsMode.isBlank() || "DUMMY".equalsIgnoreCase(paymentsMode.trim());
    }

    private void confirmPaid(Booking booking, Payment payment, String gateway, String gatewayReference) {
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

        return mapToResponse(payment, null);
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
        Booking booking = bookingRepository.findById(bookingId)
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

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Venue");
            table.addCell("Court / Sport");
            table.addCell("Date & Time");
            table.addCell("Amount (LKR)");

            table.addCell(invoice.getVenueName());
            table.addCell(invoice.getCourtName() + " / " + booking.getSport().getName());
            table.addCell(booking.getBookingDate() + " (" + booking.getStartTime() + " - " + booking.getEndTime() + ")");
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
}
