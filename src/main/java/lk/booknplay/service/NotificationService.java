package lk.booknplay.service;

import lk.booknplay.entity.Booking;
import lk.booknplay.entity.Customer;

public interface NotificationService {
    void sendBookingNotification(Customer customer, Booking booking, String eventType);
}
