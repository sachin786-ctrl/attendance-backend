package com.example.demo.repositories;

import com.example.demo.entities.RefreshToken;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // ✅ JWT ID se refresh token dhundhna (token refresh + logout ke liye)
    Optional<RefreshToken> findByJti(String jti);

    // ✅ User ke saare refresh tokens delete karo (logout all devices ke liye)
    void deleteAllByUser(User user);
}