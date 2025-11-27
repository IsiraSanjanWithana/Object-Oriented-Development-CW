package com.teammate.logic;

import com.teammate.models.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;

public class TeamMatcher implements Callable<List<Team>> {

    private List<Participant> pool;
    private int targetTeamSize;
    private static final int MAX_SAME_GAME = 2;

    public TeamMatcher(List<Participant> participants, int teamSize) {
        this.pool = new ArrayList<>(participants);
        this.targetTeamSize = teamSize;
    }

    @Override
    public List<Team> call() {
        System.out.println("Thread [" + Thread.currentThread().getName() + "] started team formation (Size " + targetTeamSize + ")...");
        return formTeams();
    }

    public List<Team> formTeams() {
        if (targetTeamSize < 3) return new ArrayList<>();

        // 1. Prepare Pools
        List<Participant> leaders = new ArrayList<>();
        List<Participant> thinkers = new ArrayList<>();
        List<Participant> balanced = new ArrayList<>();

        // Shuffle for randomness foundation
        Collections.shuffle(pool);

        for (Participant p : pool) {
            if (p.getPersonalityType() == PersonalityType.LEADER) leaders.add(p);
            else if (p.getPersonalityType() == PersonalityType.THINKER) thinkers.add(p);
            else balanced.add(p);
        }

        Comparator<Participant> skillSorter = (p1, p2) -> p2.getSkillLevel() - p1.getSkillLevel();
        leaders.sort(skillSorter);
        thinkers.sort(skillSorter);
        balanced.sort(skillSorter);

        int totalTeams = pool.size() / targetTeamSize;
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= totalTeams; i++) teams.add(new Team(i));

        // 2. Build Nucleus (Leader -> Thinker -> Balanced)
        distributeRound(teams, leaders, 1, true);
        distributeRound(teams, thinkers, 1, false);
        distributeRound(teams, balanced, 1, true);

        // 3. Fill Remaining Spots (Zig-Zag / Snake)
        List<Participant> leftovers = new ArrayList<>();
        leftovers.addAll(leaders);
        leftovers.addAll(thinkers);
        leftovers.addAll(balanced);
        leftovers.sort(skillSorter);

        boolean forward = false;
        while (!leftovers.isEmpty()) {
            boolean assignedAny = false;
            int start = forward ? 0 : teams.size() - 1;
            int end = forward ? teams.size() : -1;
            int step = forward ? 1 : -1;

            for (int i = start; i != end; i += step) {
                Team team = teams.get(i);
                if (team.getMembers().size() >= targetTeamSize) continue;

                Participant bestFit = findBestFit(team, leftovers);
                if (bestFit != null) {
                    team.addMember(bestFit);
                    leftovers.remove(bestFit);
                    assignedAny = true;
                }
            }
            if (!assignedAny) break;
            forward = !forward;
        }

        System.out.println("Initial Draft Complete. Starting Global Balancing...");

        // 4. GLOBAL VARIANCE BALANCING (The New Logic)
        globalVarianceBalance(teams);

        // Re-sort by ID for clean display
        teams.sort(Comparator.comparingInt(Team::getTeamId));
        return teams;
    }

    // ----------------------------------------------------------------
    // OPTIMIZATION LOGIC (Swapping)
    // ----------------------------------------------------------------

    private void globalVarianceBalance(List<Team> teams) {
        if (teams.size() < 2) return;

        boolean improved = true;
        int safety = 5000; // Prevent infinite loops

        while (improved && safety-- > 0) {
            improved = false;
            double currentVariance = varianceOfAverages(teams);

            // Iterate through every pair of teams
            outer:
            for (int i = 0; i < teams.size(); i++) {
                for (int j = i + 1; j < teams.size(); j++) {
                    Team t1 = teams.get(i);
                    Team t2 = teams.get(j);

                    // Try to swap every member of T1 with every member of T2
                    List<Participant> members1 = new ArrayList<>(t1.getMembers());
                    List<Participant> members2 = new ArrayList<>(t2.getMembers());

                    for (Participant p1 : members1) {
                        for (Participant p2 : members2) {

                            // 1. Constraint: Personality Preservation
                            // We only swap if they match types OR if one is Balanced (to keep Leader/Thinker nucleus safe)
                            // Ideally, swap exact types to be perfectly safe.
                            if (p1.getPersonalityType() != p2.getPersonalityType()) {
                                // Relaxed Check: Ensure we don't leave a team with 0 Leaders or 0 Thinkers
                                if (!checkPersonalitySafety(t1, t2, p1, p2)) continue;
                            }

                            // 2. Constraint: Game Cap & Role Diversity
                            if (!isValidSwap(t1, t2, p1, p2)) continue;

                            // 3. Check if Variance Improves
                            double newAvg1 = calculateAvgAfterSwap(t1, p1, p2);
                            double newAvg2 = calculateAvgAfterSwap(t2, p2, p1);

                            List<Double> hypotheticalAvgs = new ArrayList<>();
                            for (int k=0; k<teams.size(); k++) {
                                if (k == i) hypotheticalAvgs.add(newAvg1);
                                else if (k == j) hypotheticalAvgs.add(newAvg2);
                                else hypotheticalAvgs.add(teams.get(k).getAverageSkill());
                            }

                            double newVariance = calculateVariance(hypotheticalAvgs);

                            // If variance decreases significantly, perform the swap
                            if (newVariance < currentVariance - 0.0001) {
                                t1.getMembers().remove(p1);
                                t2.getMembers().remove(p2);
                                t1.addMember(p2);
                                t2.addMember(p1);

                                improved = true;
                                break outer; // Restart scan from top to see new opportunities
                            }
                        }
                    }
                }
            }
        }
    }

    // Helper: Would swapping these two break the rules?
    private boolean isValidSwap(Team t1, Team t2, Participant pFrom1, Participant pFrom2) {
        // Simulating T1 losing pFrom1 and gaining pFrom2
        // Simulating T2 losing pFrom2 and gaining pFrom1

        // 1. Check Game Limits
        long t1GameCount = t1.getMembers().stream()
                .filter(p -> p != pFrom1) // Remove pFrom1
                .filter(p -> p.getPreferredGame().equalsIgnoreCase(pFrom2.getPreferredGame())) // Count pFrom2 game
                .count();
        if (t1GameCount + 1 > MAX_SAME_GAME) return false;

        long t2GameCount = t2.getMembers().stream()
                .filter(p -> p != pFrom2)
                .filter(p -> p.getPreferredGame().equalsIgnoreCase(pFrom1.getPreferredGame()))
                .count();
        if (t2GameCount + 1 > MAX_SAME_GAME) return false;

        // 2. Check Role Diversity
        if (!checkRoleSafety(t1, pFrom1, pFrom2)) return false;
        if (!checkRoleSafety(t2, pFrom2, pFrom1)) return false;

        return true;
    }

    private boolean checkRoleSafety(Team t, Participant remove, Participant add) {
        Set<Role> roles = new HashSet<>();
        for (Participant p : t.getMembers()) {
            if (p != remove) roles.add(p.getPreferredRole());
        }
        roles.add(add.getPreferredRole());
        // Constraint: Must have at least 3 distinct roles
        return roles.size() >= 3;
    }

    private boolean checkPersonalitySafety(Team t1, Team t2, Participant p1, Participant p2) {
        // Ensure T1 still has a Leader and Thinker
        if (!hasTypeAfterSwap(t1, p1, p2, PersonalityType.LEADER)) return false;
        if (!hasTypeAfterSwap(t1, p1, p2, PersonalityType.THINKER)) return false;
        // Ensure T2 still has a Leader and Thinker
        if (!hasTypeAfterSwap(t2, p2, p1, PersonalityType.LEADER)) return false;
        if (!hasTypeAfterSwap(t2, p2, p1, PersonalityType.THINKER)) return false;
        return true;
    }

    private boolean hasTypeAfterSwap(Team t, Participant remove, Participant add, PersonalityType type) {
        long count = t.getMembers().stream().filter(p -> p.getPersonalityType() == type).count();
        if (remove.getPersonalityType() == type) count--;
        if (add.getPersonalityType() == type) count++;
        return count > 0;
    }

    private double calculateAvgAfterSwap(Team t, Participant remove, Participant add) {
        double sum = t.getMembers().stream().mapToInt(Participant::getSkillLevel).sum();
        sum = sum - remove.getSkillLevel() + add.getSkillLevel();
        return sum / t.getMembers().size();
    }

    private double varianceOfAverages(List<Team> teams) {
        List<Double> avgs = teams.stream().map(Team::getAverageSkill).collect(Collectors.toList());
        return calculateVariance(avgs);
    }

    private double calculateVariance(List<Double> values) {
        double mean = values.stream().mapToDouble(d -> d).average().orElse(0.0);
        double sumSq = 0.0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return sumSq / values.size();
    }

    // ----------------------------------------------------------------
    // STANDARD HELPERS (Kept from previous version)
    // ----------------------------------------------------------------

    private Participant findBestFit(Team team, List<Participant> candidates) {
        int currentUnique = team.getUniqueRoleCount();
        int neededRoles = 3 - currentUnique;
        int slotsRemaining = targetTeamSize - team.getMembers().size();
        boolean criticalRoleNeed = (neededRoles > 0 && slotsRemaining <= neededRoles);

        for (Participant p : candidates) {
            boolean gameOk = team.getGameCount(p.getPreferredGame()) < MAX_SAME_GAME;
            boolean isNewRole = team.contributesNewRole(p.getPreferredRole());
            if (gameOk && isNewRole) return p;
        }
        if (!criticalRoleNeed) {
            for (Participant p : candidates) {
                if (team.getGameCount(p.getPreferredGame()) < MAX_SAME_GAME) return p;
            }
        }
        for (Participant p : candidates) {
            if (team.getGameCount(p.getPreferredGame()) < MAX_SAME_GAME) return p;
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void distributeRound(List<Team> teams, List<Participant> candidates, int countPerTeam, boolean forward) {
        int start = forward ? 0 : teams.size() - 1;
        int end = forward ? teams.size() : -1;
        int step = forward ? 1 : -1;

        for (int i = start; i != end; i += step) {
            Team team = teams.get(i);
            int added = 0;
            Iterator<Participant> it = candidates.iterator();
            while (it.hasNext() && added < countPerTeam) {
                Participant p = it.next();
                if (team.getGameCount(p.getPreferredGame()) < MAX_SAME_GAME && team.contributesNewRole(p.getPreferredRole())) {
                    team.addMember(p); it.remove(); added++;
                }
            }
            if (added < countPerTeam) {
                it = candidates.iterator();
                while (it.hasNext() && added < countPerTeam) {
                    Participant p = it.next();
                    if (team.getGameCount(p.getPreferredGame()) < MAX_SAME_GAME) {
                        team.addMember(p); it.remove(); added++;
                    }
                }
            }
        }
    }
}