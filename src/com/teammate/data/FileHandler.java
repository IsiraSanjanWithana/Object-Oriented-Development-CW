package com.teammate.data;

import com.teammate.models.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {

    public static List<Participant> loadParticipants(String filePath) throws DataException {
        List<Participant> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true; // Skip header

            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }

                String[] data = line.split(",");
                if (data.length < 8) continue;

                try {
                    String id = data[0].trim();
                    String name = data[1].trim();
                    String email = data[2].trim();
                    String game = data[3].trim();
                    int skill = Integer.parseInt(data[4].trim());
                    Role role = Role.fromString(data[5].trim());
                    int score = Integer.parseInt(data[6].trim());
                    PersonalityType type = PersonalityType.valueOf(data[7].trim().toUpperCase());

                    if (role != null) {
                        list.add(new Participant(id, name, email, game, skill, role, score, type));
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Skipping invalid row -> " + line);
                }
            }
        } catch (IOException e) {
            throw new DataException("Error reading file: " + filePath, e);
        }
        return list;
    }

    public static void saveTeams(String filePath, List<Team> teams) throws DataException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("TeamID,MemberID,Name,Role,Type,Game\n");
            int tId = 1;
            for (Team t : teams) {
                for (Participant p : t.getMembers()) {
                    String line = String.format("%d,%s,%s,%s,%s,%s",
                            tId, p.getId(), p.getName(), p.getPreferredRole(), p.getPersonalityType(), p.getPreferredGame());
                    bw.write(line);
                    bw.newLine();
                }
                tId++;
            }
        } catch (IOException e) {
            throw new DataException("Error writing teams to file.", e);
        }
    }

    // === NEW METHOD: APPEND SINGLE PARTICIPANT ===
    public static void appendParticipant(String filePath, Participant p) throws DataException {
        // 'true' in FileWriter constructor enables append mode
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(p.toCSV()); // Relies on Participant.toCSV()
            bw.newLine();
        } catch (IOException e) {
            throw new DataException("Error appending participant to file.", e);
        }
    }

    public static void saveAllParticipants(String filePath, List<Participant> participants) throws DataException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // Write Header
            bw.write("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType");
            bw.newLine();

            // Write All Rows
            for (Participant p : participants) {
                bw.write(p.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new DataException("Error updating participant file.", e);
        }
    }
}