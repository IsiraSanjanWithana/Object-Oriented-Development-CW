package com.teammate.models;

public enum Role {
    STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR;

    // Helper to parse case-insensitive CSV data
    public static Role fromString(String text) {
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(text.trim())) return r;
        }
        return null; // Handle null gracefully in validation
    }
}