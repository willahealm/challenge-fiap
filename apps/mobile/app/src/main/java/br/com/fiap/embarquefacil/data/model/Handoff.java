package br.com.fiap.embarquefacil.data.model;

public class Handoff {
    public String token;
    public String expiresAt;
    public String code;

    public String displayCode() {
        if (code != null && !code.isBlank()) return code;
        if (token == null) return "------";
        String compact = token.replaceAll("[^A-Za-z0-9]", "").toUpperCase(java.util.Locale.ROOT);
        return compact.substring(0, Math.min(6, compact.length()));
    }
}
