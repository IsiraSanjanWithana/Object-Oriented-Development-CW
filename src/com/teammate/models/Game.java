package com.teammate.models;

import java.util.Arrays;
import java.util.List;

public class Game {
    // Define constants to prevent typos
    public static final String VALORANT = "Valorant";
    public static final String DOTA_2 = "Dota 2";
    public static final String FIFA = "FIFA";
    public static final String CSGO = "CS:GO";
    public static final String BASKETBALL = "Basketball";
    public static final String CHESS = "Chess";

    // The central list of all valid games
    public static final List<String> VALID_GAMES = Arrays.asList(
            VALORANT, DOTA_2, FIFA, CSGO, BASKETBALL, CHESS
    );

    // Helper to check if a string matches a known game
    public static boolean isValid(String input) {
        for (String g : VALID_GAMES) {
            if (g.equalsIgnoreCase(input)) return true;
        }
        return false;
    }

    // Helper to format input/ case check (e.g. "dota 2" -> "Dota 2")
    public static String normalize(String input) {
        for (String g : VALID_GAMES) {
            if (g.equalsIgnoreCase(input)) return g;
        }
        return input;
    }
}
