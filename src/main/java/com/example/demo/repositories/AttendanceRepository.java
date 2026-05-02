package com.example.demo.repositories;


import com.example.demo.entities.Attendance;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    // Aaj ki attendance check karo
    Optional<Attendance> findByUserAndDate(User user, LocalDate date);

    // User ki saari attendance
    List<Attendance> findByUserOrderByDateDesc(User user);

    // Monthly attendance
    List<Attendance> findByUserAndDateBetweenOrderByDateDesc(
            User user, LocalDate start, LocalDate end
    );

    // Exist check
    boolean existsByUserAndDate(User user, LocalDate date);
}
