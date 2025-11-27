package com.teammate.models;

public class Participant extends Person {
    private String preferredGame;
    private int skillLevel;
    private Role preferredRole;
    private int personalityScore;
    private PersonalityType personalityType;

    public Participant(String id, String name, String email, String game, int skill,
                       Role role, int score, PersonalityType type) {
        super(id, name, email);
        this.preferredGame = game;
        this.skillLevel = skill;
        this.preferredRole = role;
        this.personalityScore = score;
        this.personalityType = type;
    }

    @Override
    public String getDetails() {
        return String.format("%s (%s) - %s [%s]", name, preferredGame, personalityType, preferredRole);
    }

    // Getters
    public String getId() { return id; }
    public String getPreferredGame() { return preferredGame; }
    public int getSkillLevel() { return skillLevel; }
    public Role getPreferredRole() { return preferredRole; }
    public PersonalityType getPersonalityType() { return personalityType; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return getDetails();
    }

    // Formats data for CSV writing
    public String toCSV() {
        return String.join(",", id, name, email, preferredGame,
                String.valueOf(skillLevel), preferredRole.toString(),
                String.valueOf(personalityScore), personalityType.toString());
    }
}