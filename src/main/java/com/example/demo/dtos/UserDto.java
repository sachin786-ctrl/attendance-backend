package com.example.demo.dtos;

import com.example.demo.entities.Provider;
import com.example.demo.entities.Role;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// ✅ User data transfer object - request aur response dono ke liye use hota hai
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    // ✅ Password response mein null set karo (sensitive data hide karne ke liye)
    private String password;

    private Boolean enable;

    private String image;          // Profile picture URL
    private Instant createAt;
    private Instant updateAt;

    @Builder.Default
    private Provider provider = Provider.LOCAL;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}