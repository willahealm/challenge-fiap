package br.com.fiap.embarquefacil.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import br.com.fiap.embarquefacil.BuildConfig;
import br.com.fiap.embarquefacil.data.model.AuthSession;
import br.com.fiap.embarquefacil.data.model.User;

public class SessionStore {
    private static final String PREFS = "embarque_facil_prefs";
    private static final String KEY_ALIAS = "embarque_facil_session_key";
    private final SharedPreferences prefs;

    public SessionStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(AuthSession session, String email) {
        prefs.edit()
                .putString("token_cipher", encrypt(session.accessToken))
                .putString("email", email)
                .putString("user_name", session.user == null ? "Passageiro" : session.user.name)
                .putString("user_id", session.user == null ? "" : session.user.id)
                .apply();
    }

    public AuthSession getSession() {
        String token = decrypt(prefs.getString("token_cipher", null));
        if (token == null) return null;
        User user = new User();
        user.id = prefs.getString("user_id", "");
        user.name = prefs.getString("user_name", "Passageiro");
        user.email = prefs.getString("email", "");
        return new AuthSession(token, user);
    }

    public void clearSession() {
        prefs.edit().remove("token_cipher").remove("email").remove("user_name").remove("user_id").apply();
    }

    public String getBaseUrl() {
        String url = prefs.getString("api_url", BuildConfig.DEFAULT_API_URL);
        return normalizeUrl(url);
    }

    public void setBaseUrl(String value) {
        prefs.edit().putString("api_url", normalizeUrl(value)).apply();
    }

    public boolean isDemoMode() {
        return prefs.getBoolean("demo_mode", BuildConfig.DEMO_MODE_DEFAULT);
    }

    public void setDemoMode(boolean enabled) {
        prefs.edit().putBoolean("demo_mode", enabled).apply();
    }

    public boolean isValidBaseUrl(String value) {
        if (value == null) return false;
        Uri uri = Uri.parse(value.trim());
        return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && !uri.getHost().isBlank();
    }

    public SharedPreferences preferences() {
        return prefs;
    }

    private String normalizeUrl(String value) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty()) result = BuildConfig.DEFAULT_API_URL;
        return result.endsWith("/") ? result : result + "/";
    }

    private String encrypt(String value) {
        if (value == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey());
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            ByteBuffer payload = ByteBuffer.allocate(1 + iv.length + encrypted.length);
            payload.put((byte) iv.length).put(iv).put(encrypted);
            return Base64.encodeToString(payload.array(), Base64.NO_WRAP);
        } catch (Exception error) {
            throw new IllegalStateException("Não foi possível proteger a sessão local.", error);
        }
    }

    private String decrypt(String encoded) {
        if (encoded == null) return null;
        try {
            ByteBuffer payload = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP));
            byte[] iv = new byte[payload.get() & 0xff];
            payload.get(iv);
            byte[] encrypted = new byte[payload.remaining()];
            payload.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, sessionKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception error) {
            prefs.edit().remove("token_cipher").apply();
            return null;
        }
    }

    private SecretKey sessionKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
