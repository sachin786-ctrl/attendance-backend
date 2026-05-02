package com.example.demo.services;

import com.example.demo.dtos.UserDto;

import java.util.UUID;

public interface UserService {

    // ✅ Naya user create karo
    UserDto createUser(UserDto userDto);

    // ✅ Existing user update karo
    UserDto updateUser(UserDto userDto, UUID id);

    // ✅ User delete karo by ID
    void deleteUser(UUID id);

    // ✅ User dhundo    by UUID
    UserDto getUserById(UUID id);

    // ✅ User dhundo by email
    UserDto getUserByEmail(String email);
}