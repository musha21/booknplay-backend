package lk.booknplay.service;

public interface SmsLenzService {

    /**
     * Send an SMS to a normalized E.164 contact via SMSlenz.
     *
     * @param phone   normalized phone (e.g. +94771234567)
     * @param message SMS body (must not include secrets beyond the OTP itself)
     */
    void sendSms(String phone, String message);
}
