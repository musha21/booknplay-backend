package lk.booknplay.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response body structure returned when an exception occurs.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success;
    private String code;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public static ApiErrorResponse of(String code, String message) {
        return ApiErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiErrorResponse of(String code, String message, Map<String, String> errors) {
        return ApiErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
