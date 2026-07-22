package com.smartestate.security;

import com.smartestate.model.ApiToken;
import com.smartestate.model.User;
import com.smartestate.repo.ApiTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Issues, validates and revokes the opaque bearer tokens used by the mobile REST API. The raw
 * token is a 256-bit random string returned to the client once at login; only its SHA-256 hash is
 * persisted, so the stored value can't be replayed. Tokens expire and can be revoked (logout / lost
 * phone), which a stateless JWT could not do without extra machinery.
 */
@Service
public class MobileTokenService {

    /** How long a login stays valid before the app must sign in again. */
    private static final long TTL_DAYS = 90;

    private final ApiTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    public MobileTokenService(ApiTokenRepository repository) {
        this.repository = repository;
    }

    /** Mint a new token for {@code user}, persist its hash, and return the RAW token (shown once). */
    @Transactional
    public String issue(User user, String deviceLabel) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        LocalDateTime now = LocalDateTime.now();
        ApiToken entity = new ApiToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(token));
        entity.setCreatedAt(now);
        entity.setLastUsedAt(now);
        entity.setExpiresAt(now.plusDays(TTL_DAYS));
        entity.setDeviceLabel(deviceLabel);
        repository.save(entity);
        return token;
    }

    /** Resolve a raw bearer token to its user, or empty if unknown/revoked/expired. */
    @Transactional
    public Optional<User> resolve(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        Optional<ApiToken> found = repository.findByTokenHashAndRevokedFalse(sha256Hex(rawToken));
        if (found.isEmpty()) return Optional.empty();
        ApiToken t = found.get();
        if (t.getExpiresAt() != null && t.getExpiresAt().isBefore(LocalDateTime.now())) {
            t.setRevoked(true);
            return Optional.empty();
        }
        t.setLastUsedAt(LocalDateTime.now());
        return Optional.of(t.getUser());
    }

    /** Revoke a raw bearer token (logout). No-op if it's unknown. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHashAndRevokedFalse(sha256Hex(rawToken))
                .ifPresent(t -> t.setRevoked(true));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // never happens on a standard JRE
        }
    }
}
