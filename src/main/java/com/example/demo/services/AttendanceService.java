package com.example.demo.services;


import com.example.demo.dtos.AttendanceDto;
import java.util.List;

public interface AttendanceService {
    AttendanceDto checkIn(String email);
    AttendanceDto checkOut(String email);
    AttendanceDto getToday(String email);
    List<AttendanceDto> getHistory(String email);
    List<AttendanceDto> getMonthly(String email, int month, int year);
}
