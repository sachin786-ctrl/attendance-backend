package com.example.demo.Helper;

import java.util.UUID;

// ✅ Utility class - common helper methods
public class UserHelper {

    private UserHelper() {
        // Utility class - instantiate mat karo
    }

    // ✅ String UUID ko UUID object mein convert karo
    public static UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuid);
        }
    }
}