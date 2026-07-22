package com.smartestate.web.api;

import com.smartestate.repo.EmergencyEventRepository;
import com.smartestate.web.api.dto.EmergencyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the app ask "is there an active emergency right now?" on foreground/launch, so it can show
 * the alarm banner even if the push arrived while the app was killed. Returns 204 when all clear.
 */
@RestController
@RequestMapping("/api/mobile/emergency")
public class MobileEmergencyController {

    private final EmergencyEventRepository emergencyEventRepository;

    public MobileEmergencyController(EmergencyEventRepository emergencyEventRepository) {
        this.emergencyEventRepository = emergencyEventRepository;
    }

    @GetMapping("/active")
    public ResponseEntity<EmergencyDto> active() {
        return emergencyEventRepository.findFirstByActiveTrueOrderByDeclaredAtDesc()
                .map(e -> ResponseEntity.ok(EmergencyDto.of(e)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
