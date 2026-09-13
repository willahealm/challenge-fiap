package br.com.fiap.embarquefacil.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Formatters {
    private Formatters() {}

    public static String departure(String value) {
        if (value == null || value.isBlank()) return "Horário não informado";
        try {
            return DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm", new Locale("pt", "BR"))
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    public static String status(String value) {
        if (value == null) return "Status indisponível";
        return switch (value) {
            case "ON_TIME" -> "No horário";
            case "DELAYED" -> "Atrasada";
            case "BOARDING" -> "Embarcando";
            case "COMPLETED", "EMBARQUE CONCLUÍDO" -> "Embarque concluído";
            default -> value.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }
}
