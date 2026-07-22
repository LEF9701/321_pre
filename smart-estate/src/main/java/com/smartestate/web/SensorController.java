package com.smartestate.web;

import com.smartestate.iot.SensorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Live IoT sensor dashboard (management + admin, per the /mgmt/** rule). Renders the current sensor
 * grid; the page then streams updates from {@code /mgmt/iot/stream}. A fire alert can be escalated to
 * an emergency in one click, by committee (management) as well as admins, since either may need to act.
 */
@Controller
@RequestMapping("/mgmt/iot")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("sensors", sensorService.allViews());
        return "mgmt/iot";
    }

    @PostMapping("/sensors/{id}/declare")
    public String declare(@PathVariable Long id, RedirectAttributes ra) {
        String location = sensorService.declareFromSensor(id);
        Flash.msg(ra, "iot.emergencyDeclared",
                "Emergency declared for " + location + ". The system is now in elevated state.", location);
        return "redirect:/mgmt/iot";
    }
}
