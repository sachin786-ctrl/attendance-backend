package com.example.demo.services.Impl;


import com.example.demo.dtos.AttendanceDto;
import com.example.demo.entities.Attendance;
import com.example.demo.entities.AttendanceStatus;
import com.example.demo.entities.User;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.AttendanceRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 0); // 9am ke baad = LATE

    // ==================== CHECK IN ====================
    @Override
    public AttendanceDto checkIn(String email) {
        User user = getUser(email);
        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByUserAndDate(user, today)) {
            throw new IllegalStateException("Already checked in today!");
        }

        LocalTime now = LocalTime.now();
        AttendanceStatus status = now.isAfter(LATE_THRESHOLD)
                ? AttendanceStatus.LATE
                : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .user(user)
                .date(today)
                .checkIn(now)
                .status(status)
                .build();

        return toDto(attendanceRepository.save(attendance));
    }

    // ==================== CHECK OUT ====================
    @Override
    public AttendanceDto checkOut(String email) {
        User user = getUser(email);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByUserAndDate(user, today)
                .orElseThrow(() -> new IllegalStateException("Please check in first!"));

        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out today!");
        }

        attendance.setCheckOut(LocalTime.now());
        return toDto(attendanceRepository.save(attendance));
    }

    // ==================== TODAY ====================
    @Override
    @Transactional(readOnly = true)
    public AttendanceDto getToday(String email) {
        User user = getUser(email);
        return attendanceRepository.findByUserAndDate(user, LocalDate.now())
                .map(this::toDto)
                .orElse(null); // Aaj check-in nahi kiya
    }

    // ==================== HISTORY ====================
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getHistory(String email) {
        User user = getUser(email);
        return attendanceRepository.findByUserOrderByDateDesc(user)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ==================== MONTHLY ====================
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getMonthly(String email, int month, int year) {
        User user = getUser(email);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return attendanceRepository.findByUserAndDateBetweenOrderByDateDesc(user, start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ==================== HELPERS ====================
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AttendanceDto toDto(Attendance a) {
        long totalMinutes = 0;
        if (a.getCheckIn() != null && a.getCheckOut() != null) {
            totalMinutes = java.time.Duration.between(a.getCheckIn(), a.getCheckOut()).toMinutes();
        }
        return AttendanceDto.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .date(a.getDate())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .status(a.getStatus())
                .totalHours(totalMinutes)
                .build();
    }
}