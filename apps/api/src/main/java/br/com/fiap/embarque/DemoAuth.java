package br.com.fiap.embarque;

import static br.com.fiap.embarque.Models.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service @Profile("demo")
public class DemoAuth {
    private record Account(Actor actor, String passwordHash) {}
    private record Session(Actor actor, Instant expiresAt, Instant hardExpiresAt) {}
    private record Window(Instant start, int count) {}
    private final Map<String,Account> accounts = new HashMap<>();
    private final Map<String,Session> sessions = new HashMap<>();
    private final Map<String,Window> attempts = new HashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    public DemoAuth(Clock clock) {
        this.clock=clock;
        account("lucas@demo.local","Demo123!",new Actor("user-lucas","Lucas","PASSENGER",null,null));
        account("ana@demo.local","Demo123!",new Actor("user-ana","Ana","PASSENGER",null,null));
        account("operador@demo.local","Operador123!",new Actor("operator-demo","Operador demo","OPERATOR",null,null));
        account("totem@demo.local","Totem123!",new Actor("device-demo","Totem Tietê","TOTEM",null,"totem-tiete-01"));
    }
    private void account(String email,String password,Actor actor) { accounts.put(email,new Account(actor,encoder.encode(password))); }
    public synchronized AuthResponse login(Login login,String ip) {
        limit("login:"+ip,20);
        var account=accounts.get(login.email().toLowerCase(Locale.ROOT));
        // Match a real hash even for unknown accounts to avoid a fast user enumeration path.
        String hash=account==null ? accounts.get("lucas@demo.local").passwordHash():account.passwordHash();
        if(!encoder.matches(login.password(),hash) || account==null) throw unauthorized();
        return issue(account.actor(),Duration.ofHours(2));
    }
    public synchronized AuthResponse issue(Actor actor,Duration duration) {
        sessions.values().removeIf(s -> !clock.instant().isBefore(s.expiresAt()));
        String token=randomToken(); Instant expires=clock.instant().plus(duration);
        sessions.put(hash(token),new Session(actor,expires,actor.role().equals("KIOSK")?clock.instant().plusSeconds(300):expires));
        return new AuthResponse(token,"Bearer",expires,new User(actor.id(),actor.name(),actor.role()));
    }
    public synchronized Actor authenticate(String token) {
        Session session=sessions.get(hash(token));
        if(session==null || !clock.instant().isBefore(session.expiresAt())) { sessions.remove(hash(token)); throw unauthorized(); }
        return session.actor();
    }
    public synchronized void revoke(String token) { sessions.remove(hash(token)); }
    /** Only explicit human activity should call this; background polling must not renew a kiosk. */
    public synchronized Map<String,Instant> touch(String token) {
        var actor=authenticate(token);if(!actor.role().equals("KIOSK")) throw ApiException.forbidden();
        var key=hash(token);var session=sessions.get(key);var expiry=clock.instant().plusSeconds(30);
        if(expiry.isAfter(session.hardExpiresAt())) expiry=session.hardExpiresAt();
        sessions.put(key,new Session(actor,expiry,session.hardExpiresAt()));return Map.of("expiresAt",expiry);
    }
    public synchronized void revokeAll() { sessions.clear(); }
    public synchronized void limit(String key,int maximum) {
        var now=clock.instant(); attempts.values().removeIf(w -> !now.isBefore(w.start().plusSeconds(60)));
        var w=attempts.getOrDefault(key,new Window(now,0));
        if(w.count()>=maximum) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMITED","Aguarde um minuto e tente novamente.");
        attempts.put(key,new Window(w.start(),w.count()+1));
    }
    public String randomToken() { byte[] bytes=new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public String shortCode() {
        String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";var result=new StringBuilder(6);
        for(int i=0;i<6;i++) result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return result.toString();
    }
    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static ApiException unauthorized() { return new ApiException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sessão inválida ou expirada."); }
}
