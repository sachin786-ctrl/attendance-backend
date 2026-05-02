package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users") //  Table name explicitly diya
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder //  Builder pattern for OAuth2 user creation
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    //  Password nullable hai kyunki OAuth2 users ka password nahi hota
    private String password;

    //  Account enabled hai ya nahi
    @Builder.Default
    private Boolean enable = true;

    //  FIX: image Boolean tha, String hona chahiye (URL store karne ke liye)
    @Lob
    private String image;

    @Column(updatable = false)
    private Instant createAt;

    private Instant updateAt;

    //  JPA lifecycle hooks - auto timestamps
    @PrePersist
    protected void onCreate() {
        createAt = Instant.now();
        updateAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateAt = Instant.now();
    }

    //  Provider track karta hai login method
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Provider provider = Provider.LOCAL;

    //  Provider ka unique ID (Google sub, GitHub id)
    private String providerId;

    //  Roles - EAGER fetch so Security context mein available rahe
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>(Set.of(Role.USER));

    // ==================== UserDetails Implementation ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //  "ROLE_" prefix automatically add hoga - Spring Security requirement
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return email; //  Email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        //  Null-safe check
        return this.enable != null && this.enable;
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendance> attendances;
}