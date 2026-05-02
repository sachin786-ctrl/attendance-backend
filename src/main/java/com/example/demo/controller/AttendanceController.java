package com.example.demo.controller;

import com.example.demo.dtos.AttendanceDto;
import com.example.demo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceDto> checkIn(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.checkIn(userDetails.getUsername()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceDto> checkOut(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.checkOut(userDetails.getUsername()));
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceDto> getToday(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getToday(userDetails.getUsername()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttendanceDto>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(attendanceService.getHistory(userDetails.getUsername()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<AttendanceDto>> getMonthly(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getMonthly(
                userDetails.getUsername(), month, year));
    }
}