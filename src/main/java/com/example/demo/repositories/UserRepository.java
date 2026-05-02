package com.example.demo.repositories;

import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ✅ Email se user dhundhna (login + duplicate check ke liye)
    Optional<User> findByEmail(String email);

    // ✅ Email already exist karta hai? (registration check ke liye)
    boolean existsByEmail(String email);
}