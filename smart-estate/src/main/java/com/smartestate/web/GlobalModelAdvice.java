package com.smartestate.web;

import com.smartestate.model.EmergencyEvent;
import com.smartestate.model.User;
import com.smartestate.service.CurrentUserService;
import com.smartestate.service.EmergencyService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Injects the current user and active-emergency banner into every page. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;
    private final EmergencyService emergencyService;

    public GlobalModelAdvice(CurrentUserService currentUserService, EmergencyService emergencyService) {
        this.currentUserService = currentUserService;
        this.emergencyService = emergencyService;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUserService.get();
    }

    @ModelAttribute("activeEmergency")
    public EmergencyEvent activeEmergency() {
        return emergencyService.active().orElse(null);
    }
}
