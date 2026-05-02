package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

// ✅ Sabhi errors ka uniform format - client ko ek consistent error structure milega
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ Null fields response mein nahi aayenge
public class ApiError {

    private int status;        // HTTP status code (401, 403, 404, 500)
    private String error;      // Short error name ("Unauthorized", "Not Found")
    private String message;    // Detailed error message
    private String path;       // Kaun sa endpoint hit hua
    private Instant timestamp; // Kab error hua

    // ✅ Quick factory method
    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}