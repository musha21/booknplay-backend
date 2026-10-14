package lk.booknplay.util;

import java.util.List;
import java.util.Locale;

public final class SportCatalog {

    /**
     * Plain, human-readable seed names. Distinct sports are kept separate
     * (e.g. Football is NOT merged into Futsal).
     */
    public static final List<String> CANONICAL = List.of(
            "Cricket",
            "Badminton",
            "Futsal",
            "Football",
            "Basketball",
            "Volleyball",
            "Table Tennis",
            "Squash",
            "Padel",
            "8-Ball Pool",
            "Swimming"
    );

    private SportCatalog() {
    }

    /**
     * Maps a raw user-supplied sport label to a clean canonical name. Only clear
     * aliases are collapsed; genuinely custom sports (e.g. "Carrom") are returned
     * trimmed so they can be created as their own catalog entries.
     */
    public static String canonicalName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Badminton";
        }
        String n = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (n.contains("padel")) {
            return "Padel";
        }
        if (n.contains("table tennis") || n.contains("ping pong") || n.contains("ping")) {
            return "Table Tennis";
        }
        if (n.contains("tennis")) {
            return "Tennis";
        }
        if (n.contains("cricket")) {
            return "Cricket";
        }
        if (n.contains("badminton")) {
            return "Badminton";
        }
        if (n.contains("futsal") || n.contains("indoor football")) {
            return "Futsal";
        }
        if (n.contains("football") || n.contains("soccer")) {
            return "Football";
        }
        if (n.contains("basket")) {
            return "Basketball";
        }
        if (n.contains("volley")) {
            return "Volleyball";
        }
        if (n.contains("squash")) {
            return "Squash";
        }
        if (n.contains("pool") && !n.contains("swim")) {
            return "8-Ball Pool";
        }
        if (n.contains("swim") || n.contains("lane")) {
            return "Swimming";
        }
        return raw.trim();
    }

    public static String resourceLabel(String sportName) {
        String canonical = canonicalName(sportName);
        return switch (canonical) {
            case "Futsal", "Football" -> "Pitch";
            case "Table Tennis" -> "Table";
            case "8-Ball Pool" -> "Pool Table";
            case "Swimming" -> "Lane";
            default -> "Court";
        };
    }

    public static String unitName(String sportName, int index) {
        return resourceLabel(sportName) + " " + (char) ('A' + index);
    }
}
