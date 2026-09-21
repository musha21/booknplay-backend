package lk.booknplay.util;

import java.util.List;
import java.util.Locale;

public final class SportCatalog {

    public static final List<String> CANONICAL = List.of(
            "Indoor Cricket",
            "Badminton",
            "Futsal / Indoor Football",
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

    public static String canonicalName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Badminton";
        }
        String n = raw.trim().toLowerCase(Locale.ROOT);
        if (n.contains("indoor cricket") || (n.contains("cricket") && !n.contains("outdoor"))) {
            return "Indoor Cricket";
        }
        if (n.contains("badminton")) {
            return "Badminton";
        }
        if (n.contains("futsal") || n.contains("indoor football") || n.contains("football") || n.contains("soccer")) {
            return "Futsal / Indoor Football";
        }
        if (n.contains("basket")) {
            return "Basketball";
        }
        if (n.contains("volley")) {
            return "Volleyball";
        }
        if (n.contains("table tennis") || n.contains("ping")) {
            return "Table Tennis";
        }
        if (n.contains("squash")) {
            return "Squash";
        }
        if (n.contains("padel")) {
            return "Padel";
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
            case "Futsal / Indoor Football" -> "Pitch";
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
