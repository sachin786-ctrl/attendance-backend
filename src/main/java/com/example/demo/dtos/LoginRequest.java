package com.example.demo.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// ✅ Login request body - email + password
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}