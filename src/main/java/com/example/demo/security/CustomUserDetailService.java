package com.example.demo.security;

import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// ✅ Spring Security ko batao user DB se kaise load karna hai
// AuthenticationManager internally ishe use karta hai login ke time
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ✅ Email se user dhundo, nahi mila to BadCredentials throw karo
        // (UsernameNotFoundException ki jagah BadCredentials use karo - security reason:
        //  attacker ko pata nahi chalega ki email exist karta hai ya nahi)
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    }
}