package com.teammate;

import com.teammate.data.DataException;
import com.teammate.data.FileHandler;
import com.teammate.logic.SurveyModule;
import com.teammate.logic.TeamMatcher;
import com.teammate.models.Participant;
import com.teammate.models.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TeamMateApp {
    private static List<Participant> allParticipants = new ArrayList<>();
    private static final String CSV_FILE = "src/com/teammate/resources/participants_sample.csv";
    private static final String OUTPUT_FILE = "src/com/teammate/resources/formed_teams.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Initial Load (Best practice: load immediately on start)
        try {
            allParticipants = FileHandler.loadParticipants(CSV_FILE);
        } catch (DataException e) {
            System.out.println("Note: No existing data loaded.");
        }

        System.out.println("=================================================");
        System.out.println("   TeamMate: Intelligent Team Formation System   ");
        System.out.println("=================================================");

        while (true) {
            System.out.println("\n1. Reload Participants (CSV)");
            System.out.println("2. New Member Survey");
            System.out.println("3. Form Teams");
            System.out.println("4. Save Teams & Exit");
            System.out.println("5. Delete Member by ID"); // NEW OPTION
            System.out.print("Select Option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    try {
                        allParticipants = FileHandler.loadParticipants(CSV_FILE);
                        System.out.println("Loaded " + allParticipants.size() + " participants.");
                    } catch (DataException e) {
                        System.err.println(e.getMessage());
                    }
                    break;

                case "2":
                    SurveyModule survey = new SurveyModule();
                    // Generate Unique ID based on current list
                    String newId = generateNextId();

                    Participant p = survey.runSurvey(scanner, newId);

                    allParticipants.add(p);
                    try {
                        FileHandler.appendParticipant(CSV_FILE, p);
                        System.out.println("Success! Saved to CSV.");
                    } catch (DataException e) {
                        System.err.println("Warning: Failed to save to file: " + e.getMessage());
                    }
                    break;

                case "3":
                    if (allParticipants.isEmpty()) {
                        System.out.println("Error: No participants loaded!");
                        break;
                    }

                    int n = 0;
                    while (n < 3) {
                        System.out.print("Enter Team Size (N) [Minimum 3]: ");
                        try {
                            n = Integer.parseInt(scanner.nextLine());
                            if (n < 3) System.out.println("Size must be >= 3.");
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number.");
                        }
                    }

                    if (allParticipants.size() % n != 0) {
                        System.out.println("[ERROR] Cannot equally divide " + allParticipants.size() + " by " + n + ".");
                        break;
                    }

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    TeamMatcher matcher = new TeamMatcher(allParticipants, n);
                    Future<List<Team>> futureTeams = executor.submit(matcher);

                    try {
                        System.out.print("Processing...");
                        while(!futureTeams.isDone()) {
                            System.out.print(".");
                            Thread.sleep(200);
                        }
                        System.out.println("\n");
                        List<Team> formedTeams = futureTeams.get();

                        if (!formedTeams.isEmpty()) {
                            for(Team t : formedTeams) System.out.println(t);
                            FileHandler.saveTeams(OUTPUT_FILE, formedTeams);
                            System.out.println("Teams saved to " + OUTPUT_FILE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        executor.shutdown();
                    }
                    break;

                case "4":
                    System.out.println("Exiting...");
                    return;

                case "5": // === NEW DELETE FUNCTIONALITY ===
                    System.out.print("Enter Member ID to delete (e.g., P001): ");
                    String deleteId = scanner.nextLine().trim();

                    boolean removed = allParticipants.removeIf(member -> member.getId().equalsIgnoreCase(deleteId));

                    if (removed) {
                        System.out.println("Member " + deleteId + " removed from memory.");
                        try {
                            // Rewrite the entire CSV to reflect deletion
                            FileHandler.saveAllParticipants(CSV_FILE, allParticipants);
                            System.out.println("File updated successfully.");
                        } catch (DataException e) {
                            System.err.println("Error updating file: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Error: Member ID not found.");
                    }
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // Helper: Logic to maintain ID consistency (P001, P002... P101)
    private static String generateNextId() {
        int maxId = 0;
        for (Participant p : allParticipants) {
            try {
                // Extract number from "P001" -> 1
                if (p.getId().startsWith("P")) {
                    int num = Integer.parseInt(p.getId().substring(1));
                    if (num > maxId) maxId = num;
                }
            } catch (NumberFormatException ignored) {
                // Ignore weird IDs (e.g. "U-1234") if mixed data
            }
        }
        // Format new ID as P + 3 digits (e.g., P099 -> P100)
        return String.format("P%03d", maxId + 1);
    }
}