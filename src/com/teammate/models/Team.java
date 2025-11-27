package com.teammate.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Team {
    private int teamId;
    private List<Participant> members;

    public Team(int id) {
        this.teamId = id;
        this.members = new ArrayList<>();
    }

    public int getTeamId() {
        return teamId;
    }

    public void addMember(Participant p) {
        members.add(p);
    }

    public List<Participant> getMembers() { return members; }

    // === NEW HELPER METHODS FOR CONSTRAINTS ===

    // Returns how many members already play this game
    public int getGameCount(String game) {
        int count = 0;
        for (Participant p : members) {
            if (p.getPreferredGame().equalsIgnoreCase(game)) count++;
        }
        return count;
    }

    // Returns number of unique roles currently in the team
    public int getUniqueRoleCount() {
        Set<Role> roles = new HashSet<>();
        for (Participant p : members) roles.add(p.getPreferredRole());
        return roles.size();
    }

    // Check if adding this player adds a NEW role to the team
    public boolean contributesNewRole(Role r) {
        for (Participant p : members) {
            if (p.getPreferredRole() == r) return false;
        }
        return true;
    }

    public double getAverageSkill() {
        return members.stream().mapToInt(Participant::getSkillLevel).average().orElse(0.0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== TEAM %d (Avg: %.2f | Roles: %d) ===", teamId, getAverageSkill(), getUniqueRoleCount()));
        for (Participant p : members) {
            sb.append("\n  -> ").append(p.getDetails());
        }
        return sb.toString();
    }
}