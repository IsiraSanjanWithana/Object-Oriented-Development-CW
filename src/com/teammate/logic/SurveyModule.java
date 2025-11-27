package com.teammate.logic;

import com.teammate.models.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SurveyModule {

    // Uses the centralized list from Game.java
    private static final List<String> VALID_GAMES = Game.VALID_GAMES;

    // CHANGED: Now accepts 'newId' passed from the Main App
    public Participant runSurvey(Scanner scanner, String newId) {
        System.out.println("\n--- NEW MEMBER SURVEY (" + newId + ") ---");

        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        // 1. Email Validation
        String email = "";
        while (true) {
            System.out.print("Enter Email: ");
            email = scanner.nextLine().trim();
            if (email.contains("@") && email.contains(".") && email.length() > 3) break;
            System.out.println("Error: Invalid email. Must contain '@'and '.'");
        }

        // 2. Interest Validation
        String game = "";
        while (true) {
            System.out.println("Select Main Interest: " + VALID_GAMES);
            String input = scanner.nextLine().trim();

            // Use Game class to validate
            if (Game.isValid(input)) {
                game = Game.normalize(input); // Saves as "Valorant" even if typed "valorant"
                break;
            }
            System.out.println("Error: Invalid game selection.");
        }

        // 3. Skill Validation
        int skill = 0;
        while(skill < 1 || skill > 10) {
            System.out.print("Rate your skill (1-10): ");
            try {
                skill = Integer.parseInt(scanner.nextLine());
                if (skill < 1 || skill > 10) System.out.println("Error: 1-10 only.");
            }
            catch (NumberFormatException e) { System.out.println("Error: Numbers only."); }
        }

        // 4. Role Validation
        Role role = null;
        while(role == null) {
            System.out.println("Preferred Role (Strategist, Attacker, Defender, Supporter, Coordinator): ");
            role = Role.fromString(scanner.nextLine());
            if(role == null) System.out.println("Error: Invalid role.");
        }

        // 5. Personality Questions
        System.out.println("Rate 1 (Disagree) to 5 (Agree):");
        int score = 0;
        score += ask(scanner, "I enjoy taking the lead.");
        score += ask(scanner, "I prefer analyzing situations.");
        score += ask(scanner, "I work well with others.");
        score += ask(scanner, "I am calm under pressure.");
        score += ask(scanner, "I like making quick decisions.");

        int totalScore = score * 4;
        PersonalityType type = PersonalityType.fromScore(totalScore);

        System.out.println("Survey Complete! ID Assigned: " + newId);

        return new Participant(newId, name, email, game, skill, role, totalScore, type);
    }

    private int ask(Scanner sc, String q) {
        int val = 0;
        while (val < 1 || val > 5) {
            System.out.print(q + ": ");
            try {
                val = Integer.parseInt(sc.nextLine());
                if (val < 1 || val > 5) System.out.println("Error: 1-5 only.");
            } catch (NumberFormatException e) { System.out.println("Error: Invalid input."); }
        }
        return val;
    }
}