package com.smartestate.web.api;

import com.smartestate.model.User;
import com.smartestate.repo.UserRepository;
import com.smartestate.security.MobileTokenService;
import com.smartestate.service.CurrentUserService;
import com.smartestate.web.api.dto.LoginRequest;
import com.smartestate.web.api.dto.LoginResponse;
import com.smartestate.web.api.dto.MeDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Mobile authentication: exchange email + password for an opaque bearer token, look up the current
 * user, and revoke the token on logout. This chain is stateless and CSRF-exempt (see SecurityConfig)
 * because a native client can't participate in the session-cookie + CSRF flow the web UI uses.
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MobileTokenService tokenService;
    private final CurrentUserService currentUserService;

    public MobileAuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                MobileTokenService tokenService, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   @RequestHeader(value = "X-Device-Label", required = false) String deviceLabel) {
        User user = userRepository.findByEmail(req.email().trim()).orElse(null);
        if (user == null || !user.isEnabled()
                || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid credentials"));
        }
        String token = tokenService.issue(user, deviceLabel);
        return ResponseEntity.ok(new LoginResponse(token, MeDto.of(user)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenService.revoke(header.substring(7).trim());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        User user = currentUserService.get();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(MeDto.of(user));
    }

    /** Minimal error envelope so the app gets JSON, not an HTML error page. */
    public record ErrorResponse(String error) {}
}
