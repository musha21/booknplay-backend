package lk.booknplay.service.impl;

import lk.booknplay.dto.request.BookingCreateRequest;
import lk.booknplay.dto.response.BookingResponse;
import lk.booknplay.entity.*;
import lk.booknplay.enums.BookingStatus;
import lk.booknplay.enums.CourtStatus;
import lk.booknplay.enums.PaymentStatus;

import lk.booknplay.exception.*;
import lk.booknplay.repository.*;
import lk.booknplay.service.BookingService;
import lk.booknplay.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final CourtRepository courtRepository;
    private final SportRepository sportRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(String customerEmail, BookingCreateRequest request) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        // 1. Acquire pessimistic lock on court row to prevent concurrent booking race conditions
        Court court = courtRepository.findByIdWithLock(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id: " + request.getCourtId()));

        if (court.getStatus() != CourtStatus.ACTIVE) {
            throw new BadRequestException("Court is currently inactive");
        }

        Sport sport = sportRepository.findById(request.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id: " + request.getSportId()));

        // 2. Double-check slot availability inside transaction
        boolean isOverlapping = bookingRepository.existsOverlappingBooking(
                court.getId(),
                request.getDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (isOverlapping) {
            throw new ConflictException("COURT_ALREADY_BOOKED", "The court is already booked for the selected time slot.");
        }

        // 3. Calculate total amount
        long hours = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toHours();
        if (hours <= 0) hours = 1;
        BigDecimal totalAmount = court.getHourlyRate().multiply(BigDecimal.valueOf(hours));

        String bookingRef = "BNP-" + request.getDate().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Booking booking = Booking.builder()
                .bookingRef(bookingRef)
                .customer(customer)
                .court(court)
                .venue(court.getVenue())
                .sport(sport)
                .bookingDate(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .source(lk.booknplay.enums.BookingSource.ONLINE)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        Payment payment = Payment.builder()
                .booking(savedBooking)
                .customer(customer)
                .amount(totalAmount)
                .currency("LKR")
                .status(PaymentStatus.INITIATED)
                .build();

        paymentRepository.save(payment);

        notificationService.sendBookingNotification(customer, savedBooking, "BOOKING_CREATED");

        return mapToResponse(savedBooking, PaymentStatus.INITIATED);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String customerEmail, String bookingId) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied to this booking");
        }

        PaymentStatus paymentStatus = paymentRepository.findByBookingId(booking.getId())
                .map(Payment::getStatus)
                .orElse(PaymentStatus.INITIATED);

        return mapToResponse(booking, paymentStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(String customerEmail, Pageable pageable) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getUpcomingBookings(String customerEmail, Pageable pageable) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        return bookingRepository.findByCustomerIdAndBookingDateGreaterThanEqualAndStatusOrderByBookingDateAscStartTimeAsc(
                customer.getId(), LocalDate.now(), BookingStatus.CONFIRMED, pageable
        ).map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingHistory(String customerEmail, Pageable pageable) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        List<BookingStatus> historyStatuses = List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);
        return bookingRepository.findByCustomerIdAndBookingDateLessThanAndStatusInOrderByBookingDateDescStartTimeDesc(
                customer.getId(), LocalDate.now(), historyStatuses, pageable
        ).map(b -> mapToResponse(b, getPaymentStatus(b.getId())));
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String customerEmail, String bookingId) {
        Customer customer = customerRepository.findByUser_Email(customerEmail)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("Access denied to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        // Must cancel at least 24 hours prior to booking start time
        LocalDateTime bookingStartDateTime = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (LocalDateTime.now().plusHours(24).isAfter(bookingStartDateTime)) {
            throw new BadRequestException("Bookings can only be cancelled at least 24 hours in advance");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);

        PaymentStatus pStatus = paymentRepository.findByBookingId(booking.getId()).map(p -> {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                p.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(p);
                return PaymentStatus.REFUNDED;
            }
            return p.getStatus();
        }).orElse(PaymentStatus.INITIATED);

        notificationService.sendBookingNotification(customer, updated, "BOOKING_CANCELLED");

        return mapToResponse(updated, pStatus);
    }

    private PaymentStatus getPaymentStatus(String bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(Payment::getStatus)
                .orElse(PaymentStatus.INITIATED);
    }

    private BookingResponse mapToResponse(Booking booking, PaymentStatus paymentStatus) {
        String customerId = booking.getCustomer() != null ? booking.getCustomer().getId() : null;
        String customerName = booking.getCustomer() != null
                ? booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName()
                : booking.getGuestName();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingRef(booking.getBookingRef())
                .customerId(customerId)
                .customerName(customerName)
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .sportId(booking.getSport().getId())
                .sportName(booking.getSport().getName())
                .date(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalAmount(booking.getTotalAmount())
                .currency("LKR")
                .status(booking.getStatus())
                .paymentStatus(paymentStatus)
                .source(booking.getSource())
                .guestName(booking.getGuestName())
                .guestPhone(booking.getGuestPhone())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
