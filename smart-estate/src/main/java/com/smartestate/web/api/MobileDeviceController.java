package com.smartestate.web.api;

import com.smartestate.model.DeviceToken;
import com.smartestate.model.Platform;
import com.smartestate.model.PushEnvironment;
import com.smartestate.model.User;
import com.smartestate.repo.DeviceTokenRepository;
import com.smartestate.service.CurrentUserService;
import com.smartestate.web.api.dto.DeviceRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Device-token registry for push. The app calls POST after every APNs token issue/refresh, and
 * DELETE on logout/uninstall. Registration is upsert-by-token, so a token that moves to a new
 * account re-homes to the current user (a shared/handed-down phone won't leak the old resident's
 * alerts). All routes require a valid bearer token (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/mobile/devices")
public class MobileDeviceController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final CurrentUserService currentUserService;

    public MobileDeviceController(DeviceTokenRepository deviceTokenRepository,
                                  CurrentUserService currentUserService) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody DeviceRegistrationRequest req) {
        User user = currentUserService.get();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        DeviceToken dt = deviceTokenRepository.findByToken(req.token().trim()).orElseGet(DeviceToken::new);
        LocalDateTime now = LocalDateTime.now();
        if (dt.getCreatedAt() == null) dt.setCreatedAt(now);
        dt.setUser(user);
        dt.setToken(req.token().trim());
        dt.setPlatform(parsePlatform(req.platform()));
        dt.setEnvironment(parseEnvironment(req.environment()));
        dt.setAppVersion(req.appVersion());
        dt.setActive(true);
        dt.setLastSeenAt(now);
        deviceTokenRepository.save(dt);

        return ResponseEntity.ok(Map.of("id", dt.getId(), "active", true));
    }

    @DeleteMapping("/{token}")
    @Transactional
    public ResponseEntity<Void> unregister(@PathVariable String token) {
        User user = currentUserService.get();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        deviceTokenRepository.findByToken(token.trim())
                .filter(dt -> dt.getUser() != null && dt.getUser().getId().equals(user.getId()))
                .ifPresent(dt -> dt.setActive(false));
        return ResponseEntity.noContent().build();
    }

    private static Platform parsePlatform(String value) {
        if (value == null || value.isBlank()) return Platform.IOS;
        try { return Platform.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return Platform.IOS; }
    }

    private static PushEnvironment parseEnvironment(String value) {
        if (value == null || value.isBlank()) return PushEnvironment.SANDBOX;
        try { return PushEnvironment.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return PushEnvironment.SANDBOX; }
    }
}
