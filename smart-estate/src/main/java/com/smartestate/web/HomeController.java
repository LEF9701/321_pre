package com.smartestate.web;

import com.smartestate.model.Role;
import com.smartestate.model.User;
import com.smartestate.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CurrentUserService currentUserService;

    public HomeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Send each role to its own landing page after login. */
    @GetMapping("/")
    public String home() {
        User u = currentUserService.get();
        if (u == null) return "redirect:/login";
        if (u.getRole() == Role.RESIDENT) return "redirect:/resident";
        if (u.getRole() == Role.MANAGEMENT) return "redirect:/mgmt";
        if (u.getRole() == Role.ADMIN) return "redirect:/admin";
        return "redirect:/login";
    }
}
