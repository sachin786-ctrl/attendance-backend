package com.example.demo.dtos;

import com.example.demo.entities.AttendanceStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceDto {
    private UUID id;
    private UUID userId;
    private LocalDate date;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private AttendanceStatus status;
    private long totalHours; // minutes mein
}