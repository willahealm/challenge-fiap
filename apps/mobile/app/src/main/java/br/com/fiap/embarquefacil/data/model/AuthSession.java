package br.com.fiap.embarquefacil.data.model;

public class AuthSession {
    public String accessToken;
    public String tokenType;
    public String expiresAt;
    public User user;

    public AuthSession() {}

    public AuthSession(String accessToken, User user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.user = user;
    }
}
