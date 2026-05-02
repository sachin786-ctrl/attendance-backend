package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// ✅ Refresh tokens ko DB mein store karte hain taaki revoke kar sakein
@Entity
@Table(name = "refresh_tokens", indexes = {
        // ✅ jti se fast lookup ke liye index
        @Index(name = "idx_refresh_tokens_jti", columnList = "jti", unique = true),
        // ✅ User ke saare tokens dhundhne ke liye index
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ✅ JWT ID - token ka unique identifier (JWT ka "jti" claim)
    @Column(name = "jti", unique = true, nullable = false, updatable = false)
    private String jti;

    // ✅ Kaun sa user hai - ManyToOne (ek user ke multiple refresh tokens ho sakte hain)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    // ✅ Kab banaya gaya
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    // ✅ Kab expire hoga
    @Column(nullable = false)
    private Instant expiresAt;

    // ✅ Token revoke kiya gaya hai ya nahi (logout pe true hoga)
    @Column(nullable = false)
    private boolean revoked;

    // ✅ Rotation: purana token kis naye token se replace hua (optional tracking)
    private String replacedByJti;
}