package lk.booknplay.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message) {
        super(message);
        this.code = "CONFLICT";
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
