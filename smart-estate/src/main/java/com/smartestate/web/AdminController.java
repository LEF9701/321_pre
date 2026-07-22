package com.smartestate.web;

import com.smartestate.model.*;
import com.smartestate.repo.*;
import com.smartestate.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BuildingRepository buildingRepo;
    private final BlockRepository blockRepo;
    private final FloorRepository floorRepo;
    private final UnitRepository unitRepo;
    private final UserRepository userRepo;
    private final IssueRepository issueRepo;
    private final AuditLogRepository auditRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final FloorMapService floorMapService;
    private final AuditService auditService;
    private final PasswordEncoder encoder;

    public AdminController(BuildingRepository buildingRepo, BlockRepository blockRepo, FloorRepository floorRepo,
                           UnitRepository unitRepo, UserRepository userRepo, IssueRepository issueRepo,
                           AuditLogRepository auditRepo, NotificationPreferenceRepository prefRepo,
                           FloorMapService floorMapService, AuditService auditService, PasswordEncoder encoder) {
        this.buildingRepo = buildingRepo;
        this.blockRepo = blockRepo;
        this.floorRepo = floorRepo;
        this.unitRepo = unitRepo;
        this.userRepo = userRepo;
        this.issueRepo = issueRepo;
        this.auditRepo = auditRepo;
        this.prefRepo = prefRepo;
        this.floorMapService = floorMapService;
        this.auditService = auditService;
        this.encoder = encoder;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("buildingCount", buildingRepo.count());
        model.addAttribute("blockCount", blockRepo.count());
        model.addAttribute("unitCount", unitRepo.count());
        model.addAttribute("userCount", userRepo.count());
        return "admin/home";
    }

    // ---- Buildings (FR-4.1) ----------------------------------------------

    @GetMapping("/buildings")
    public String buildings(Model model) {
        List<BuildingRow> rows = new ArrayList<>();
        for (Building b : buildingRepo.findAllByOrderByNameAsc()) {
            List<Block> blocks = blockRepo.findByBuilding_IdOrderByBlockNameAsc(b.getId());
            int units = unitRepo.findByFloor_Block_Building_Id(b.getId()).size();
            rows.add(new BuildingRow(b, blocks.size(), units));
        }
        model.addAttribute("rows", rows);
        return "admin/buildings";
    }

    @PostMapping("/buildings")
    public String createBuilding(@RequestParam String name, @RequestParam(required = false) String address,
                                 @RequestParam(required = false) String district, RedirectAttributes ra) {
        Building b = new Building();
        b.setName(name);
        b.setAddress(address);
        b.setDistrict(district);
        buildingRepo.save(b);
        auditService.log("BUILDING_CREATE", "Registered building " + name);
        Flash.msg(ra, "building.registered", "Building \"" + name + "\" registered.", name);
        return "redirect:/admin/buildings/" + b.getId();
    }

    @PostMapping("/buildings/{id}/delete")
    public String deleteBuilding(@PathVariable Long id, RedirectAttributes ra) {
        buildingRepo.findById(id).ifPresent(b -> {
            for (Block bl : blockRepo.findByBuilding_IdOrderByBlockNameAsc(id)) deleteBlockCascade(bl);
            buildingRepo.delete(b);
            auditService.log("BUILDING_DELETE", "Deleted building " + b.getName());
        });
        Flash.msg(ra, "building.deleted", "Building deleted.");
        return "redirect:/admin/buildings";
    }

    @GetMapping("/buildings/{id}")
    public String buildingDetail(@PathVariable Long id, Model model) {
        Building b = buildingRepo.findById(id).orElseThrow();
        List<BlockRow> rows = new ArrayList<>();
        for (Block bl : blockRepo.findByBuilding_IdOrderByBlockNameAsc(id)) {
            int floors = floorRepo.findByBlockOrderByFloorNumberAsc(bl).size();
            int units = unitRepo.findByFloor_Block_IdOrderByUnitCodeAsc(bl.getId()).size();
            rows.add(new BlockRow(bl, floors, units));
        }
        model.addAttribute("building", b);
        model.addAttribute("rows", rows);
        return "admin/building_detail";
    }

    // ---- Blocks / floors / units (FR-4.2, FR-4.3) ------------------------

    @PostMapping("/buildings/{id}/blocks")
    public String addBlock(@PathVariable Long id, @RequestParam String blockName,
                           @RequestParam(required = false) String blockType,
                           @RequestParam(defaultValue = "0") int floors,
                           @RequestParam(defaultValue = "0") int unitsPerFloor, RedirectAttributes ra) {
        Building b = buildingRepo.findById(id).orElseThrow();
        Block block = new Block();
        block.setBuilding(b);
        block.setBlockName(blockName);
        block.setBlockType(blockType);
        blockRepo.save(block);
        int created = generateFloorsAndUnits(block, floors, unitsPerFloor);
        auditService.log("BLOCK_CREATE", "Added block " + blockName + " (" + floors + " floors x "
                + unitsPerFloor + " units = " + created + " units)");
        Flash.msg(ra, "block.added", "Block \"" + blockName + "\" added with " + created + " units.",
                blockName, created);
        return "redirect:/admin/blocks/" + block.getId();
    }

    @GetMapping("/blocks/{id}")
    public String blockDetail(@PathVariable Long id, Model model) {
        Block block = blockRepo.findById(id).orElseThrow();
        List<FloorView> floors = new ArrayList<>();
        List<Floor> fl = floorRepo.findByBlockOrderByFloorNumberAsc(block);
        fl.sort(Comparator.comparingInt(Floor::getFloorNumber).reversed());
        for (Floor f : fl) {
            floors.add(new FloorView(f, unitRepo.findByFloorOrderByUnitCodeAsc(f)));
        }
        model.addAttribute("block", block);
        model.addAttribute("floors", floors);
        return "admin/block_detail";
    }

    @PostMapping("/blocks/{id}/floors")
    public String addFloor(@PathVariable Long id, @RequestParam int floorNumber,
                           @RequestParam(defaultValue = "0") int unitsPerFloor, RedirectAttributes ra) {
        Block block = blockRepo.findById(id).orElseThrow();
        Floor f = new Floor();
        f.setBlock(block);
        f.setFloorNumber(floorNumber);
        floorRepo.save(f);
        for (int i = 1; i <= unitsPerFloor; i++) createUnit(f, String.format("%02d", i));
        auditService.log("FLOOR_CREATE", "Added floor " + floorNumber + " to " + block.getBlockName());
        Flash.msg(ra, "floor.added", "Floor " + floorNumber + " added.", floorNumber);
        return "redirect:/admin/blocks/" + id;
    }

    @PostMapping("/blocks/{id}/delete")
    public String deleteBlock(@PathVariable Long id, RedirectAttributes ra) {
        Block block = blockRepo.findById(id).orElse(null);
        if (block != null) {
            Long buildingId = block.getBuilding().getId();
            deleteBlockCascade(block);
            auditService.log("BLOCK_DELETE", "Deleted block " + block.getBlockName());
            Flash.msg(ra, "block.deleted", "Block deleted.");
            return "redirect:/admin/buildings/" + buildingId;
        }
        return "redirect:/admin/buildings";
    }

    @PostMapping("/floors/{id}/units")
    public String addUnit(@PathVariable Long id, @RequestParam String unitCode, RedirectAttributes ra) {
        Floor f = floorRepo.findById(id).orElseThrow();
        createUnit(f, unitCode);
        Flash.msg(ra, "unit.added", "Unit " + unitCode + " added.", unitCode);
        return "redirect:/admin/blocks/" + f.getBlock().getId();
    }

    @PostMapping("/floors/{id}/delete")
    public String deleteFloor(@PathVariable Long id, RedirectAttributes ra) {
        Floor f = floorRepo.findById(id).orElse(null);
        if (f != null) {
            Long blockId = f.getBlock().getId();
            unitRepo.findByFloorOrderByUnitCodeAsc(f).forEach(this::detachAndDeleteUnit);
            floorRepo.delete(f);
            Flash.msg(ra, "floor.deleted", "Floor deleted.");
            return "redirect:/admin/blocks/" + blockId;
        }
        return "redirect:/admin/buildings";
    }

    @GetMapping("/units/{id}")
    public String unitDetail(@PathVariable Long id, Model model) {
        Unit unit = unitRepo.findById(id).orElseThrow();
        // A flat can house a whole household, so list every resident of the unit, not just one.
        List<User> occupants = userRepo.findByRoleOrderByNameAsc(Role.RESIDENT).stream()
                .filter(u -> u.getUnit() != null && u.getUnit().getId().equals(id)).toList();
        List<Issue> issues = issueRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> i.getUnit() != null && i.getUnit().getId().equals(id)).toList();
        model.addAttribute("unit", unit);
        model.addAttribute("occupants", occupants);
        model.addAttribute("issues", issues);
        model.addAttribute("statuses", UnitStatus.values());
        model.addAttribute("feeStatuses", FeeStatus.values());
        return "admin/unit_detail";
    }

    @PostMapping("/units/{id}")
    public String updateUnit(@PathVariable Long id, @RequestParam String unitCode, @RequestParam UnitStatus status,
                             @RequestParam FeeStatus feeStatus, @RequestParam(required = false) BigDecimal balance,
                             @RequestParam(required = false) String dueDate, RedirectAttributes ra) {
        Unit unit = unitRepo.findById(id).orElseThrow();
        unit.setUnitCode(unitCode);
        unit.setStatus(status);
        unit.setFeeStatus(feeStatus);
        unit.setBalance(balance == null ? BigDecimal.ZERO : balance);
        unit.setDueDate(dueDate == null || dueDate.isBlank() ? null : LocalDate.parse(dueDate));
        unitRepo.save(unit);
        auditService.log("UNIT_UPDATE", "Updated unit " + unit.getFullCode());
        Flash.msg(ra, "unit.updated", "Unit updated.");
        return "redirect:/admin/units/" + id;
    }

    @PostMapping("/units/{id}/delete")
    public String deleteUnit(@PathVariable Long id, RedirectAttributes ra) {
        Unit unit = unitRepo.findById(id).orElse(null);
        if (unit != null) {
            Long blockId = unit.getFloor().getBlock().getId();
            detachAndDeleteUnit(unit);
            Flash.msg(ra, "unit.deleted", "Unit deleted.");
            return "redirect:/admin/blocks/" + blockId;
        }
        return "redirect:/admin/buildings";
    }

    // ---- Floor map (FR-4.4, FR-4.5, FR-4.6) ------------------------------

    @GetMapping("/floormap")
    public String floormap(@RequestParam(required = false) Long building, Model model) {
        List<Building> buildings = buildingRepo.findAllByOrderByNameAsc();
        Long buildingId = building != null ? building : (buildings.isEmpty() ? null : buildings.get(0).getId());
        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuilding", buildingId);
        if (buildingId != null) {
            model.addAttribute("maps", floorMapService.mapsForBuilding(buildingId));
        }
        return "admin/floormap";
    }

    // ---- Users & roles (Manage User Roles) -------------------------------

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepo.findAll());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("units", unitRepo.findAll());
        model.addAttribute("isNew", true);
        return "admin/user_form";
    }

    @PostMapping("/users")
    public String createUser(@RequestParam String name, @RequestParam String email,
                             @RequestParam(required = false) String phone, @RequestParam Role role,
                             @RequestParam Language language, @RequestParam(required = false) Long unitId,
                             @RequestParam String password, RedirectAttributes ra) {
        if (userRepo.findByEmail(email).isPresent()) {
            Flash.error(ra, "user.emailInUse", "Email already in use.");
            return "redirect:/admin/users/new";
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRole(role);
        u.setLanguage(language);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode(password == null || password.isBlank() ? "changeme123" : password));
        if (role == Role.RESIDENT && unitId != null) {
            unitRepo.findById(unitId).ifPresent(unit -> {
                u.setUnit(unit);
                if (unit.getStatus() == UnitStatus.VACANT) { unit.setStatus(UnitStatus.OCCUPIED); unitRepo.save(unit); }
            });
        }
        userRepo.save(u);
        auditService.log("USER_CREATE", "Created " + role + " account " + email);
        Flash.msg(ra, "user.created", "User " + name + " created.", name);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", userRepo.findById(id).orElseThrow());
        model.addAttribute("roles", Role.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("units", unitRepo.findAll());
        model.addAttribute("isNew", false);
        return "admin/user_form";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @RequestParam String name, @RequestParam String email,
                             @RequestParam(required = false) String phone, @RequestParam Role role,
                             @RequestParam Language language, @RequestParam(required = false) Long unitId,
                             @RequestParam(required = false) String password, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElseThrow();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRole(role);
        u.setLanguage(language);
        u.setUnit(role == Role.RESIDENT && unitId != null ? unitRepo.findById(unitId).orElse(null) : null);
        if (password != null && !password.isBlank()) u.setPasswordHash(encoder.encode(password));
        userRepo.save(u);
        auditService.log("USER_UPDATE", "Updated account " + email + " (role " + role + ")");
        Flash.msg(ra, "user.updated", "User updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElseThrow();
        u.setEnabled(!u.isEnabled());
        userRepo.save(u);
        auditService.log("USER_TOGGLE", (u.isEnabled() ? "Enabled " : "Disabled ") + u.getEmail());
        if (u.isEnabled()) {
            Flash.msg(ra, "user.enabled", "Account enabled.");
        } else {
            Flash.msg(ra, "user.disabled", "Account disabled.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userRepo.findById(id).ifPresent(u -> {
            prefRepo.findByUserOrderByCategoryAsc(u).forEach(prefRepo::delete);
            userRepo.delete(u);
            auditService.log("USER_DELETE", "Deleted account " + u.getEmail());
        });
        Flash.msg(ra, "user.deleted", "User deleted.");
        return "redirect:/admin/users";
    }

    // ---- Audit log (FR-7.5 / NFR) ----------------------------------------

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String filter, Model model) {
        boolean emergencyOnly = "emergency".equals(filter);
        model.addAttribute("logs", emergencyOnly
                ? auditRepo.findByEmergencyRelatedTrueOrderByTimestampDesc()
                : auditRepo.findTop200ByOrderByTimestampDesc());
        model.addAttribute("emergencyOnly", emergencyOnly);
        return "admin/audit";
    }

    // ---- helpers ----------------------------------------------------------

    private int generateFloorsAndUnits(Block block, int floors, int unitsPerFloor) {
        int count = 0;
        for (int fn = 1; fn <= floors; fn++) {
            Floor f = new Floor();
            f.setBlock(block);
            f.setFloorNumber(fn);
            floorRepo.save(f);
            for (int un = 1; un <= unitsPerFloor; un++) {
                createUnit(f, String.format("%02d", un));
                count++;
            }
        }
        return count;
    }

    private void createUnit(Floor f, String code) {
        Unit u = new Unit();
        u.setFloor(f);
        u.setUnitCode(code);
        u.setStatus(UnitStatus.VACANT);
        u.setFeeStatus(FeeStatus.PAID);
        u.setBalance(BigDecimal.ZERO);
        unitRepo.save(u);
    }

    private void deleteBlockCascade(Block block) {
        for (Floor f : floorRepo.findByBlockOrderByFloorNumberAsc(block)) {
            unitRepo.findByFloorOrderByUnitCodeAsc(f).forEach(this::detachAndDeleteUnit);
            floorRepo.delete(f);
        }
        blockRepo.delete(block);
    }

    /** Detach any resident from a unit before deleting it to keep FK integrity. */
    private void detachAndDeleteUnit(Unit unit) {
        for (User u : userRepo.findByRole(Role.RESIDENT)) {
            if (u.getUnit() != null && u.getUnit().getId().equals(unit.getId())) {
                u.setUnit(null);
                userRepo.save(u);
            }
        }
        unitRepo.delete(unit);
    }

    // ---- view models ------------------------------------------------------

    public static class BuildingRow {
        private final Building building; private final int blocks; private final int units;
        public BuildingRow(Building b, int blocks, int units) { this.building = b; this.blocks = blocks; this.units = units; }
        public Building getBuilding() { return building; }
        public int getBlocks() { return blocks; }
        public int getUnits() { return units; }
    }

    public static class BlockRow {
        private final Block block; private final int floors; private final int units;
        public BlockRow(Block b, int floors, int units) { this.block = b; this.floors = floors; this.units = units; }
        public Block getBlock() { return block; }
        public int getFloors() { return floors; }
        public int getUnits() { return units; }
    }

    public static class FloorView {
        private final Floor floor; private final List<Unit> units;
        public FloorView(Floor f, List<Unit> units) { this.floor = f; this.units = units; }
        public Floor getFloor() { return floor; }
        public List<Unit> getUnits() { return units; }
    }
}
