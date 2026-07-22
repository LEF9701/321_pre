package com.smartestate.web;

import com.smartestate.iot.AccessService;
import com.smartestate.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Management/admin access control: the card-registration approval queue (maker-checker) and the
 * access/movement log. Both are on the /mgmt side, so residents cannot see other residents' movement
 * data. The live log is streamed via the existing {@code /mgmt/iot/stream} SSE endpoint.
 */
@Controller
@RequestMapping("/mgmt/access")
public class MgmtAccessController {

    private final AccessService accessService;
    private final CurrentUserService currentUser;

    public MgmtAccessController(AccessService accessService, CurrentUserService currentUser) {
        this.accessService = accessService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("events", accessService.recentEvents(100));
        model.addAttribute("pending", accessService.pendingRequests());
        return "mgmt/access";
    }

    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        accessService.approve(id, currentUser.displayName());
        Flash.msg(ra, "access.approved", "Card request approved.");
        return "redirect:/mgmt/access";
    }

    @PostMapping("/requests/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        accessService.reject(id, currentUser.displayName());
        Flash.msg(ra, "access.rejected", "Card request rejected.");
        return "redirect:/mgmt/access";
    }
}
