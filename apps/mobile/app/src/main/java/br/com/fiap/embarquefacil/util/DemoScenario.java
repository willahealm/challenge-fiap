package br.com.fiap.embarquefacil.util;

import java.util.Arrays;
import java.util.Locale;

public final class DemoScenario {
    private DemoScenario() {}

    public static boolean shouldPublishPlatformChange(int journeyLoads) {
        return journeyLoads >= 4;
    }

    public static boolean isValidCheckpoint(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return Arrays.asList("TERMINAL-A", "TOTEM-01", "EF-SP-18", "TIETE-A",
                "TIETE-TOTEM-01", "TIETE-ENTRADA").contains(normalized);
    }
}
