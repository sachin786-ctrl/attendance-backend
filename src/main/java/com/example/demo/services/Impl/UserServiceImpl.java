package com.example.demo.services.Impl;

import com.example.demo.dtos.UserDto;
import com.example.demo.entities.Provider;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.RefreshTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional // ✅ Sabhi write operations transactional hain by default
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    // ==================== CREATE ====================

    @Override
    public UserDto createUser(UserDto userDto) {

        // ✅ Email validation
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        // ✅ Email format check
        if (!userDto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // ✅ Duplicate email check
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + userDto.getEmail());
        }

//        // ✅ Password check
//        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
//            throw new IllegalArgumentException("Password is required");
//        }
//        if (userDto.getPassword().length() < 6) {
//            throw new IllegalArgumentException("Password must be at least 6 characters");
//        }

        User user = modelMapper.map(userDto, User.class);

        // ✅ Default role assign karo agar koi role nahi diya
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>(Set.of(Role.USER)));
        }

        // ✅ Default provider LOCAL set karo
        if (user.getProvider() == null) {
            user.setProvider(Provider.LOCAL);
        }

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    // ==================== UPDATE ====================

    @Override
    public UserDto updateUser(@NonNull UserDto userDto, UUID id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // ✅ Email change ho raha hai to duplicate check karo
        if (userDto.getEmail() != null
                && !existingUser.getEmail().equalsIgnoreCase(userDto.getEmail())
                && userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + userDto.getEmail());
        }

        // ✅ Sirf non-null fields update karo (partial update)
        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    // ==================== DELETE ====================

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // ✅ Pehle refresh tokens delete karo
        refreshTokenRepository.deleteAllByUser(user);
        userRepository.deleteById(id);
    }
    // ==================== READ ====================

    @Override
    @Transactional(readOnly = true) // ✅ Read-only = performance better hogi
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDto.class);
    }
}