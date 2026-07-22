package com.smartestate.web;

import com.smartestate.iot.AccessService;
import com.smartestate.model.CardRequestAction;
import com.smartestate.model.User;
import com.smartestate.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Resident self-service for their Octopus access card. A resident sees their own card and can
 * propose a change (add / replace / remove), but the change only takes effect once management or an
 * admin approves it (maker-checker). Residents never see other residents' cards or movement data.
 */
@Controller
@RequestMapping("/resident/access")
public class ResidentAccessController {

    private final AccessService accessService;
    private final CurrentUserService currentUser;

    public ResidentAccessController(AccessService accessService, CurrentUserService currentUser) {
        this.accessService = accessService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String page(Model model) {
        User u = currentUser.get();
        model.addAttribute("card", accessService.activeCard(u));
        model.addAttribute("requests", accessService.myRequests(u));
        return "resident/access";
    }

    @PostMapping("/request")
    public String submit(@RequestParam CardRequestAction action,
                         @RequestParam(required = false) String cardNumber, RedirectAttributes ra) {
        String number = cardNumber == null ? "" : cardNumber.trim();
        if (action != CardRequestAction.REMOVE && number.isBlank()) {
            Flash.error(ra, "access.needCard", "Please enter a card number.");
            return "redirect:/resident/access";
        }
        accessService.submitRequest(currentUser.get(), number, action);
        Flash.msg(ra, "access.requestSubmitted",
                "Your request was submitted and is awaiting management approval.");
        return "redirect:/resident/access";
    }
}
