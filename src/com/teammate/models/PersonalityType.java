package com.teammate.models;

public enum PersonalityType {
    LEADER, THINKER, BALANCED;

    public static PersonalityType fromScore(int score) {
        if (score >= 90) return LEADER;
        if (score >= 70) return BALANCED; // 70-89
        if (score >= 50) return THINKER;  // 50-69
        return BALANCED; // Default fallback for weird scores, or handle as error
    }
}