package com.smartestate.seed;

import com.smartestate.model.*;
import com.smartestate.repo.*;
import com.smartestate.service.AuditService;
import com.smartestate.service.NotificationDispatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds the in-memory database on startup with Evergreen Bay Estate data (6 Trident blocks) plus
 * demo accounts, ~48 residents, ~40 labelled issues, dispatched notification history, and a
 * past closed emergency, so every dashboard, the floor map, analytics and the AI classifier
 * are populated from the first page load. An admin can add/edit/remove anything at runtime.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final BuildingRepository buildingRepo;
    private final BlockRepository blockRepo;
    private final FloorRepository floorRepo;
    private final UnitRepository unitRepo;
    private final UserRepository userRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final IssueRepository issueRepo;
    private final MessageTemplateRepository templateRepo;
    private final MaintenanceScheduleRepository scheduleRepo;
    private final NotificationRepository notificationRepo;
    private final EmergencyEventRepository emergencyRepo;
    private final NotificationDispatchService dispatchService;
    private final AuditService auditService;
    private final PasswordEncoder encoder;

    private static final int FLOORS_PER_BLOCK = 9;
    private static final int UNITS_PER_FLOOR = 8;

    /** Seeds the ~48 fictional demo residents so every dashboard, the floor map, fees and the
     *  inbox are populated from the first page load. On by default for this prototype; set
     *  app.seed.residents=false for a public deployment that must hold no resident personal data. */
    @org.springframework.beans.factory.annotation.Value("${app.seed.residents:true}")
    private boolean seedResidents;

    public DataSeeder(BuildingRepository buildingRepo, BlockRepository blockRepo, FloorRepository floorRepo,
                      UnitRepository unitRepo, UserRepository userRepo, NotificationPreferenceRepository prefRepo,
                      IssueRepository issueRepo, MessageTemplateRepository templateRepo,
                      MaintenanceScheduleRepository scheduleRepo, NotificationRepository notificationRepo,
                      EmergencyEventRepository emergencyRepo, NotificationDispatchService dispatchService,
                      AuditService auditService, PasswordEncoder encoder) {
        this.buildingRepo = buildingRepo;
        this.blockRepo = blockRepo;
        this.floorRepo = floorRepo;
        this.unitRepo = unitRepo;
        this.userRepo = userRepo;
        this.prefRepo = prefRepo;
        this.issueRepo = issueRepo;
        this.templateRepo = templateRepo;
        this.scheduleRepo = scheduleRepo;
        this.notificationRepo = notificationRepo;
        this.emergencyRepo = emergencyRepo;
        this.dispatchService = dispatchService;
        this.auditService = auditService;
        this.encoder = encoder;
    }

    private final Random rnd = new Random(42);
    private final Map<String, List<Unit>> unitsByBlock = new LinkedHashMap<>();
    private final Set<Long> takenUnitIds = new HashSet<>();
    private final List<User> allResidents = new ArrayList<>();
    private final List<User> generatedResidents = new ArrayList<>();
    private final Channel[] channelCycle = {Channel.EMAIL, Channel.SMS, Channel.WHATSAPP, Channel.VOICE};

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        Building fuShin = new Building();
        fuShin.setName("Evergreen Bay Estate");
        fuShin.setAddress("1 Evergreen Bay Road, New Territories");
        fuShin.setDistrict("Demo District");
        buildingRepo.save(fuShin);

        Map<String, Unit> king = buildEstate(fuShin);

        createUser("Estate Administrator", "admin@smartestate.hk", "+85221112222", "admin123", Role.ADMIN, Language.EN, null);
        createUser("Evergreen Property Management Ltd.", "manager@smartestate.hk", "+85230000000", "manager123", Role.MANAGEMENT, Language.EN, null);

        // Non-personal data always seeded (no PII).
        seedTemplates();
        seedSchedules(fuShin);

        // Resident personal data (names, phones, emails) + everything derived from it.
        // OFF by default so a public deployment stores NO resident PII. Enable only for a
        // local/private demo via app.seed.residents=true.
        if (seedResidents) {
            seedNamedResidents(king);
            seedHouseholds();
            seedNamedIssues(king);
            seedLabelledIssues();
            seedNotificationHistory();
            seedPastEmergency();
        }
    }

    // ---- estate structure -------------------------------------------------

    private Map<String, Unit> buildEstate(Building fuShin) {
        String[][] blockDefs = {
                {"Azalea Tower", "Trident 1"}, {"Bauhinia Tower", "Trident 2"},
                {"Camellia Tower", "Trident 2"}, {"Dahlia Tower", "Trident 1"},
                {"Erica Tower", "Trident 2"}, {"Freesia Tower", "Trident 1"},
        };
        Map<String, Unit> king = new HashMap<>();
        for (String[] def : blockDefs) {
            Block block = new Block();
            block.setBuilding(fuShin);
            block.setBlockName(def[0]);
            block.setBlockType(def[1]);
            blockRepo.save(block);
            List<Unit> blockUnits = new ArrayList<>();

            for (int fn = 1; fn <= FLOORS_PER_BLOCK; fn++) {
                Floor floor = new Floor();
                floor.setBlock(block);
                floor.setFloorNumber(fn);
                floorRepo.save(floor);
                for (int un = 1; un <= UNITS_PER_FLOOR; un++) {
                    String code = String.format("%02d", un);
                    Unit unit = new Unit();
                    unit.setFloor(floor);
                    unit.setUnitCode(code);
                    // Every unit starts vacant with no fee data. Occupancy + fees are applied ONLY
                    // when residents are actually assigned (seedNamedResidents / seedHouseholds), so
                    // an "occupied" flat always has residents and never a phantom balance.
                    unit.setStatus(UnitStatus.VACANT);
                    unit.setFeeStatus(FeeStatus.PAID);
                    unit.setBalance(BigDecimal.ZERO);
                    unit.setDueDate(null);
                    unitRepo.save(unit);
                    blockUnits.add(unit);
                    if (def[0].equals("Azalea Tower")) king.put(fn + "-" + code, unit);
                }
            }
            unitsByBlock.put(def[0], blockUnits);
        }
        return king;
    }

    private FeeStatus rollFee() {
        int roll = rnd.nextInt(100);
        if (roll < 70) return FeeStatus.PAID;
        if (roll < 88) return FeeStatus.DUE;
        return FeeStatus.OVERDUE;
    }

    private void applyFee(Unit unit, FeeStatus fs) {
        unit.setFeeStatus(fs);
        if (fs == FeeStatus.PAID) { unit.setBalance(BigDecimal.ZERO); unit.setDueDate(null); }
        else if (fs == FeeStatus.DUE) { unit.setBalance(new BigDecimal("680.00")); unit.setDueDate(LocalDate.now().plusDays(10)); }
        else { unit.setBalance(new BigDecimal("1360.00")); unit.setDueDate(LocalDate.now().minusDays(15)); }
    }

    // ---- residents --------------------------------------------------------

    private void seedNamedResidents(Map<String, Unit> king) {
        // Several flats are shared households (same unitKey), so the demo shows more than one
        // card-holder per flat; each member gets their own Octopus card (see AccessSeeder).
        record R(String name, String email, String phone, Language lang, String unitKey, FeeStatus fee, Channel ch) {}
        List<R> residents = List.of(
                new R("Chan Tai Man", "resident@smartestate.hk", "+85291110001", Language.EN, "5-03", FeeStatus.PAID, Channel.EMAIL),
                new R("Chan Siu Ling", "chan.sl@smartestate.hk", "+85291110011", Language.ZH, "5-03", FeeStatus.PAID, Channel.SMS),
                new R("Wong Mei Ling", "wong@smartestate.hk", "+85291110002", Language.ZH, "5-04", FeeStatus.OVERDUE, Channel.SMS),
                new R("Lee Chi Wai", "lee@smartestate.hk", "+85291110003", Language.EN, "3-02", FeeStatus.DUE, Channel.WHATSAPP),
                new R("Lee Ka Yiu", "lee.ky@smartestate.hk", "+85291110013", Language.EN, "3-02", FeeStatus.DUE, Channel.EMAIL),
                new R("Cheung Ka Yan", "cheung@smartestate.hk", "+85291110004", Language.ZH, "6-01", FeeStatus.PAID, Channel.VOICE),
                new R("Ng Ho Yin", "ng@smartestate.hk", "+85291110005", Language.EN, "2-07", FeeStatus.PAID, Channel.EMAIL),
                new R("Lam Siu Fong", "lam@smartestate.hk", "+85291110006", Language.ZH, "4-05", FeeStatus.DUE, Channel.WHATSAPP),
                new R("Lam Wai Kit", "lam.wk@smartestate.hk", "+85291110016", Language.EN, "4-05", FeeStatus.DUE, Channel.SMS)
        );
        for (R r : residents) {
            Unit unit = king.get(r.unitKey());
            unit.setStatus(UnitStatus.OCCUPIED);
            applyFee(unit, r.fee());
            unitRepo.save(unit);
            User u = createUser(r.name(), r.email(), r.phone(), "resident123", Role.RESIDENT, r.lang(), unit);
            takenUnitIds.add(unit.getId());
            createPreferences(u, r.ch(), r.lang());
            allResidents.add(u);
        }
    }

    private static final String[] SURNAMES = {"Chan", "Lee", "Wong", "Cheung", "Lam", "Ng", "Ho", "Tang", "Kwok",
            "Yeung", "Tsang", "Mak", "Fung", "Lau", "Cheng", "Yip", "Tam", "Choi", "Au", "Pang"};
    private static final String[] GIVEN = {"Ka Ho", "Mei Yu", "Chun Kit", "Wing Sze", "Ho Nam", "Yan Yan",
            "Kwok Wai", "Sze Wan", "Tsz Kit", "Pui Shan", "Ming Fai", "Lai Han", "Hoi Ching", "Chi Hang", "Wai Lam"};

    /**
     * Occupies a share of the remaining flats and fills each with a HOUSEHOLD of 1–3 residents, so
     * every occupied flat has residents (never "occupied but empty") and a flat can have more than
     * one card-holder. Each resident is given their own Octopus card in {@link AccessSeeder}.
     */
    private void seedHouseholds() {
        int flat = 0;
        for (Map.Entry<String, List<Unit>> e : unitsByBlock.entrySet()) {
            for (Unit unit : e.getValue()) {
                if (takenUnitIds.contains(unit.getId())) continue;    // named flats already occupied
                if (rnd.nextInt(100) >= 22) continue;                 // ~22% of the rest are occupied
                applyFee(unit, rollFee());
                unit.setStatus(UnitStatus.OCCUPIED);
                unitRepo.save(unit);
                takenUnitIds.add(unit.getId());

                int size = householdSize();
                for (int h = 0; h < size; h++) {
                    String surname = SURNAMES[flat % SURNAMES.length];
                    String name = surname + " " + GIVEN[(flat + h * 5) % GIVEN.length];
                    String email = surname.toLowerCase() + flat + (h > 0 ? "_" + h : "") + "@smartestate.hk";
                    String phone = "+8529" + String.format("%07d", 3200000 + flat * 4 + h);
                    Language lang = ((flat + h) % 2 == 0) ? Language.EN : Language.ZH;
                    Channel ch = channelCycle[(flat + h) % channelCycle.length];
                    User u = createUser(name, email, phone, "resident123", Role.RESIDENT, lang, unit);
                    createPreferences(u, ch, lang);
                    allResidents.add(u);
                    generatedResidents.add(u);
                }
                flat++;
            }
        }
    }

    /** Household size, weighted toward 1–2 people. */
    private int householdSize() {
        int r = rnd.nextInt(100);
        if (r < 55) return 1;
        if (r < 85) return 2;
        return 3;
    }

    private void createPreferences(User u, Channel channel, Language lang) {
        for (NoticeCategory cat : NoticeCategory.values()) {
            if (cat.isForcedAllChannels()) continue;
            NotificationPreference p = new NotificationPreference();
            p.setUser(u);
            p.setCategory(cat);
            p.setChannel(channel);
            p.setLanguage(lang);
            prefRepo.save(p);
        }
    }

    private User createUser(String name, String email, String phone, String rawPw,
                            Role role, Language lang, Unit unit) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(rawPw));
        u.setRole(role);
        u.setLanguage(lang);
        u.setEnabled(true);
        u.setUnit(unit);
        return userRepo.save(u);
    }

    // ---- issues -----------------------------------------------------------

    private void seedNamedIssues(Map<String, Unit> king) {
        Map<String, User> byEmail = new HashMap<>();
        for (User u : allResidents) byEmail.put(u.getEmail(), u);
        seedIssue(byEmail.get("cheung@smartestate.hk"), IssueCategory.PLUMBING,
                "Kitchen sink has been leaking for two days, water pooling under the cabinet.",
                IssuePriority.HIGH, IssueStatus.IN_PROGRESS, "Evergreen Maintenance Team", null, 3);
        seedIssue(byEmail.get("lee@smartestate.hk"), IssueCategory.NOISE,
                "Renovation drilling next door continues past 22:00 on weekdays.",
                IssuePriority.LOW, IssueStatus.SUBMITTED, null, null, 2);
        seedIssue(byEmail.get("ng@smartestate.hk"), IssueCategory.AC_HEATING,
                "Air-conditioner condensate dripping into the common corridor.",
                IssuePriority.MEDIUM, IssueStatus.UNDER_REVIEW, "Evergreen Maintenance Team", null, 4);
        seedIssue(byEmail.get("resident@smartestate.hk"), IssueCategory.COMMON_AREA,
                "Lobby ceiling light on the ground floor is broken.",
                IssuePriority.MEDIUM, IssueStatus.RESOLVED, "Evergreen Maintenance Team", "Tube replaced on site.", 6);
    }

    private void seedLabelledIssues() {
        Map<IssueCategory, String[]> pool = new EnumMap<>(IssueCategory.class);
        pool.put(IssueCategory.PLUMBING, new String[]{
                "The kitchen tap is dripping constantly and the water pressure is very low.",
                "Toilet keeps running and will not stop filling after each flush.",
                "There is a blocked drain in the bathroom and water backs up onto the floor.",
                "The pipe under the sink is leaking and the cabinet is getting wet.",
                "No hot water is coming through in the shower this morning."});
        pool.put(IssueCategory.ELECTRICAL, new String[]{
                "A power socket in the living room is sparking when I plug things in.",
                "The main circuit breaker trips whenever I switch on the kettle.",
                "Half the sockets in the bedroom have no power at all.",
                "The doorbell stopped working and there is a faint burnt smell.",
                "Lights in my flat keep flickering and dimming randomly."});
        pool.put(IssueCategory.AC_HEATING, new String[]{
                "The air-conditioner is making a loud rattling noise and not cooling.",
                "The heater in the bedroom will not turn on at all this winter.",
                "The split-type aircon outdoor unit is vibrating very badly.",
                "Condensate from my aircon is dripping onto the unit below.",
                "The air-conditioner blows warm air even at the lowest setting."});
        pool.put(IssueCategory.SECURITY, new String[]{
                "The lobby door lock is broken and will not latch, anyone can walk in.",
                "A CCTV camera on the ground floor appears to be smashed.",
                "A stranger has been loitering in the car park late at night.",
                "My letterbox lock has been forced open.",
                "The intercom to the main gate is not working."});
        pool.put(IssueCategory.COMMON_AREA, new String[]{
                "There is graffiti on the staircase wall on the third floor.",
                "A bench in the podium garden is broken and unsafe to sit on.",
                "The glass on the ground floor notice board is cracked.",
                "Floor tiles are loose near the lift lobby entrance.",
                "The handrail on the staircase is wobbly and coming loose."});
        pool.put(IssueCategory.NOISE, new String[]{
                "Loud music from the flat upstairs every night until very late.",
                "Mahjong noise late at night from a neighbouring unit.",
                "A dog barks continuously from a flat on my floor.",
                "Banging and furniture dragging noise long after midnight.",
                "Karaoke from next door is extremely loud on weekends."});
        pool.put(IssueCategory.OTHER, new String[]{
                "There is a wasp nest forming on my window ledge.",
                "Cockroaches keep coming up through the floor drain.",
                "A pigeon is trapped in the staircase and cannot get out.",
                "The unit number painted on my door has faded, please repaint it.",
                "There is a persistent musty smell in the corridor near the refuse room."});

        IssueStatus[] statuses = IssueStatus.values();
        IssuePriority[] priorities = IssuePriority.values();
        int idx = 0;
        for (Map.Entry<IssueCategory, String[]> e : pool.entrySet()) {
            for (String desc : e.getValue()) {
                User reporter = generatedResidents.isEmpty()
                        ? null : generatedResidents.get(idx % generatedResidents.size());
                IssueStatus status = statuses[idx % statuses.length];
                IssuePriority priority = priorities[idx % priorities.length];
                String assignee = status == IssueStatus.SUBMITTED ? null : "Evergreen Maintenance Team";
                String note = status == IssueStatus.RESOLVED ? "Attended and resolved." : null;
                seedIssue(reporter, e.getKey(), desc, priority, status, assignee, note, 1 + (idx % 20));
                idx++;
            }
        }
    }

    private void seedIssue(User reporter, IssueCategory cat, String desc, IssuePriority priority,
                           IssueStatus status, String assignee, String note, int daysAgo) {
        if (reporter == null) return;
        Issue i = new Issue();
        i.setReporter(reporter);
        i.setUnit(reporter.getUnit());
        i.setCategory(cat);
        i.setDescription(desc);
        i.setPriority(priority);
        i.setStatus(status);
        i.setAssignedTo(assignee);
        i.setResolutionNote(note);
        i.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        i.setUpdatedAt(LocalDateTime.now().minusDays(Math.max(0, daysAgo - 1)));
        i = issueRepo.save(i);
        i.setRefNumber(String.format("RPT-%04d", i.getId()));
        issueRepo.save(i);
    }

    // ---- notifications history --------------------------------------------

    private void seedNotificationHistory() {
        historical("Welcome to the Smart Estate notification system",
                "歡迎使用 Smart Estate 通知系統",
                "Dear resident, Evergreen Bay Estate now uses Smart Estate for building notices. Please review your "
                        + "notification preferences so you receive notices on your preferred channel and language.",
                "各位住戶：翠灣邨現已採用 Smart Estate 發放屋邨通告，請檢視你的通知偏好設定。",
                NoticeCategory.ANNOUNCEMENT, Severity.INFO, allResidents, 21, 12);
        historical("Notice of Annual General Meeting 2026",
                "2026 年週年業主大會通告",
                "Notice is given that the Annual General Meeting of the Owners' Corporation will be held on "
                        + LocalDate.now().plusDays(30) + " at 19:30 in the Estate Community Hall. Agenda attached.",
                "茲通告業主立案法團週年大會將於 " + LocalDate.now().plusDays(30) + " 晚上7時30分假屋邨社區會堂舉行。",
                NoticeCategory.MEETING, Severity.INFO, allResidents, 14, 6);
        historical("Lift No.2 maintenance this Saturday",
                "本星期六 2 號升降機維修",
                "Lift No.2 in Azalea Tower will be out of service for scheduled maintenance this Saturday "
                        + "09:00-13:00. Lift No.1 remains in operation.",
                "善景樓2號升降機將於本星期六上午9時至下午1時暫停服務以進行維修。",
                NoticeCategory.SERVICE_INTERRUPTION, Severity.INFO, allResidents, 10, 4);
        historical("Typhoon Signal No.8 — stay indoors",
                "八號風球 — 請留在室內",
                "The Observatory has issued Typhoon Signal No.8. Please stay indoors, secure windows and avoid the "
                        + "podium. Non-emergency estate services are suspended.",
                "天文台已發出八號烈風或暴風信號，請留在室內並緊固窗戶，非緊急服務暫停。",
                NoticeCategory.OBSERVATORY, Severity.WARNING, allResidents, 8, 5);
        historical("Fresh water tank cleaning next week",
                "下星期食水缸清洗",
                "Statutory cleaning of the fresh water tanks will take place next Wednesday. Water supply will be "
                        + "suspended estate-wide from 10:00 to 14:00. Please store sufficient water in advance.",
                "下星期三將進行食水缸法定清洗，全邨食水供應將於上午10時至下午2時暫停，請預先儲水。",
                NoticeCategory.SERVICE_INTERRUPTION, Severity.WARNING, allResidents, 6, 3);
        historical("Reminder: keep corridors clear (fire safety)",
                "提提你：保持走廊暢通（消防安全）",
                "For fire safety, corridors and staircases must be kept completely clear. Please remove any shoe "
                        + "cabinets, bicycles or plants from the common areas.",
                "為消防安全，走廊及樓梯必須保持暢通，請清理擺放於公共地方的鞋櫃、單車及盆栽。",
                NoticeCategory.ANNOUNCEMENT, Severity.INFO, allResidents, 4, 7);

        // batch by block (FR-1.5): only Dahlia Tower residents
        List<User> shinMei = allResidents.stream()
                .filter(u -> u.getUnit() != null && u.getUnit().getFloor() != null
                        && "Dahlia Tower".equals(u.getUnit().getFloor().getBlock().getBlockName()))
                .toList();
        if (!shinMei.isEmpty()) {
            historical("Dahlia Tower: scheduled power maintenance",
                    "善美樓：預定電力維修",
                    "Electricity supply to Dahlia Tower common areas will be briefly suspended on Thursday 14:00-15:00 "
                            + "for switchboard inspection. Lifts may be affected.",
                    "善美樓公共地方電力將於星期四下午2時至3時短暫暫停以檢查總掣，升降機或受影響。",
                    NoticeCategory.SERVICE_INTERRUPTION, Severity.INFO, shinMei, 3, 1);
        }

        // fee reminder to residents with outstanding fees
        List<User> owing = allResidents.stream()
                .filter(u -> u.getUnit() != null && u.getUnit().getFeeStatus() != FeeStatus.PAID)
                .toList();
        if (!owing.isEmpty()) {
            historical("Monthly management fee reminder",
                    "每月管理費提醒",
                    "Our records show your management fee for this month is outstanding. Please settle the balance "
                            + "before the due date to avoid a late charge.",
                    "本紀錄顯示你本月管理費尚未繳清，請於到期日前清繳以免產生逾期費用。",
                    NoticeCategory.FEE_REMINDER, Severity.WARNING, owing, 2, 0);
        }
    }

    private void historical(String title, String titleZh, String en, String zh, NoticeCategory cat, Severity sev,
                            List<User> recipients, int daysAgo, int markReadCount) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setTitleZh(titleZh);
        n.setContentEn(en);
        n.setContentZh(zh);
        n.setCategory(cat);
        n.setSeverity(sev);
        n.setCreatedByName("Evergreen Property Management Ltd.");
        n.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        n.setRecipientScope(recipients.size() == allResidents.size() ? "All residents" : "Filtered batch");
        n.setScopeType("ALL");
        n = notificationRepo.save(n);
        dispatchService.dispatch(n, recipients, false, true); // simulateOnly: seeded demo history is never really sent
        n.setDispatchedAt(LocalDateTime.now().minusDays(daysAgo));
        notificationRepo.save(n);
        for (int i = 0; i < Math.min(markReadCount, recipients.size()); i++) {
            dispatchService.markRead(n, recipients.get(i));
        }
    }

    // ---- past (closed) emergency ------------------------------------------

    private void seedPastEmergency() {
        EmergencyEvent e = new EmergencyEvent();
        e.setType(EmergencyType.TYPHOON);
        e.setDeclaredBy("Estate Administrator");
        e.setDeclaredAt(LocalDateTime.now().minusDays(5));
        e.setClosedAt(LocalDateTime.now().minusDays(5).plusHours(6));
        e.setActive(false);
        e.setNote("Severe Typhoon — preparedness broadcast (drill)");
        emergencyRepo.save(e);

        Notification n = new Notification();
        n.setTitle("SEVERE TYPHOON — Stay indoors and away from windows");
        n.setTitleZh("強烈颱風 — 請留在室內並遠離窗戶");
        n.setContentEn("A severe typhoon is approaching Hong Kong. Stay indoors, keep away from windows, and do not go "
                + "onto the podium or rooftop. Follow all instructions from estate staff. This is a preparedness drill.");
        n.setContentZh("強烈颱風正逼近香港，請留在室內、遠離窗戶，切勿前往平台或天台，並聽從職員指示。此為演習。");
        n.setCategory(NoticeCategory.EMERGENCY);
        n.setSeverity(Severity.CRITICAL);
        n.setCreatedByName("Estate Administrator");
        n.setCreatedAt(LocalDateTime.now().minusDays(5));
        n.setRecipientScope("ALL residents (preferences overridden)");
        n.setEmergencyEvent(e);
        n = notificationRepo.save(n);
        dispatchService.dispatch(n, allResidents, true, true);       // simulateOnly: seeded drill, never real sends
        int escalated = dispatchService.runVoiceEscalation(n, true); // simulateOnly
        n.setDispatchedAt(LocalDateTime.now().minusDays(5));
        notificationRepo.save(n);

        auditService.log("Estate Administrator", "EMERGENCY_DECLARE", "Typhoon declared (drill).", true);
        auditService.log("Estate Administrator", "EMERGENCY_BROADCAST",
                "Forced broadcast to " + allResidents.size() + " residents across SMS, WhatsApp and Email.", true);
        auditService.log("Estate Administrator", "EMERGENCY_VOICE_ESCALATION",
                escalated + " voice call(s) placed to residents not reached on other channels.", true);
        auditService.log("Estate Administrator", "EMERGENCY_CLOSE", "Typhoon drill closed.", true);
    }

    // ---- templates & schedules --------------------------------------------

    private void seedSchedules(Building b) {
        seedSchedule(b, "Annual Fire Services Inspection",
                "Mandatory annual inspection of fire service installations (hydrants, hose reels, alarm panel).",
                LocalDate.now().plusDays(7), "All floors, 09:00-17:00");
        seedSchedule(b, "Fresh Water Tank Cleaning",
                "Statutory cleaning and disinfection of the potable water tanks. Water supply will be suspended.",
                LocalDate.now().plusDays(12), "Estate-wide, water suspended 10:00-14:00");
        seedSchedule(b, "Lift No.2 Modernisation",
                "Lift No.2 out of service for modernisation works; Lift No.1 remains in operation.",
                LocalDate.now().plusDays(21), "Azalea Tower, Lift No.2 only");
        seedSchedule(b, "External Wall Drainage Repair",
                "Repair of external wall drainage pipes; scaffolding will be erected on the east elevation.",
                LocalDate.now().plusDays(28), "Erica Tower, east side");
        seedSchedule(b, "Pest Control Treatment",
                "Quarterly pest control treatment of refuse rooms and common areas.",
                LocalDate.now().plusDays(15), "Refuse rooms, all blocks");
    }

    private void seedSchedule(Building b, String title, String desc, LocalDate date, String scope) {
        MaintenanceSchedule s = new MaintenanceSchedule();
        s.setBuilding(b);
        s.setTitle(title);
        s.setDescription(desc);
        s.setScheduledDate(date);
        s.setAffectedScope(scope);
        scheduleRepo.save(s);
    }

    private void seedTemplates() {
        template("OBS_T8", "Observatory: Typhoon Signal No.8", NoticeCategory.OBSERVATORY, Severity.WARNING, false,
                "Tropical Cyclone Warning Signal No.8",
                "The Hong Kong Observatory has issued Typhoon Signal No.8. Residents are advised to stay indoors, "
                        + "secure windows, and avoid the podium and open areas. Estate non-emergency services are suspended.",
                "八號烈風或暴風信號",
                "香港天文台已發出八號烈風或暴風信號。請住戶留在室內、緊固窗戶，並避免前往平台及空曠地方。屋邨非緊急服務暫停。");
        template("OBS_RAIN_BLACK", "Observatory: Black Rainstorm Warning", NoticeCategory.OBSERVATORY, Severity.WARNING, false,
                "Black Rainstorm Warning",
                "The Black Rainstorm Warning is in force. Be alert to flooding in low-lying areas of the estate and "
                        + "avoid the car park ramp. Report any water seepage to the management office.",
                "黑色暴雨警告",
                "黑色暴雨警告現正生效。請留意屋邨低窪地方水浸風險，避免使用停車場斜道。如發現滲水請通知管理處。");
        template("MEET_AGM", "Bilingual AGM / Meeting Notice (BMO)", NoticeCategory.MEETING, Severity.INFO, false,
                "Notice of General Meeting",
                "Notice is hereby given under the Building Management Ordinance (Cap. 344) that a general meeting of the "
                        + "Owners' Corporation will be held on [DATE] at [TIME] in [VENUE]. The agenda is attached. "
                        + "Owners unable to attend may appoint a proxy.",
                "業主大會通告",
                "茲根據《建築物管理條例》（第344章）通告，業主立案法團將於 [日期] [時間] 假 [地點] 舉行業主大會。議程隨附，未能出席的業主可委任代表出席。");
        template("FEE_REMIND", "Monthly Fee Reminder", NoticeCategory.FEE_REMINDER, Severity.INFO, false,
                "Management Fee Reminder",
                "This is a reminder that your monthly management fee is due. Please settle the balance before the due date.",
                "管理費繳費提示",
                "謹此提醒，您本月管理費現已到期，請於到期日前清繳。");
        template("SVC_WATER", "Service Interruption: Water Suspension", NoticeCategory.SERVICE_INTERRUPTION, Severity.WARNING, false,
                "Temporary Suspension of Water Supply",
                "Please be advised that the fresh water supply will be temporarily suspended on [DATE] from [TIME] to [TIME] "
                        + "for [REASON]. Please store sufficient water in advance.",
                "暫停食水供應",
                "謹此通知，因 [原因]，食水供應將於 [日期] [時間] 至 [時間] 暫停，請預先儲水備用。");
        template("SVC_LIFT", "Service Interruption: Lift Maintenance", NoticeCategory.SERVICE_INTERRUPTION, Severity.INFO, false,
                "Lift Maintenance Notice",
                "Lift No.[N] will be out of service for scheduled maintenance on [DATE] from [TIME] to [TIME]. "
                        + "The remaining lift(s) will continue to operate. We apologise for any inconvenience.",
                "升降機維修通知",
                "[N] 號升降機將於 [日期] [時間] 至 [時間] 進行定期維修，期間暫停服務，其餘升降機照常運作，不便之處敬請見諒。");
        template("SVC_POWER", "Service Interruption: Power Suspension", NoticeCategory.SERVICE_INTERRUPTION, Severity.WARNING, false,
                "Temporary Suspension of Electricity Supply",
                "The electricity supply to [AREA] will be temporarily suspended on [DATE] from [TIME] to [TIME] for "
                        + "essential maintenance. Please switch off sensitive equipment beforehand.",
                "暫停電力供應",
                "因進行必要維修，[範圍] 的電力供應將於 [日期] [時間] 至 [時間] 暫停，請預先關閉敏感電器。");
        template("EMG_FIRE", "EMERGENCY: Fire Alarm", NoticeCategory.EMERGENCY, Severity.CRITICAL, true,
                "FIRE ALARM — EVACUATE NOW",
                "A FIRE has been reported in [LOCATION]. Evacuate immediately using the nearest staircase. "
                        + "DO NOT use the lifts. Proceed to the assembly area at the basketball court. Follow staff instructions.",
                "火警 — 請立即疏散",
                "[地點] 發生火警。請立即使用最近的樓梯疏散，切勿使用升降機，並前往籃球場集合點。請聽從職員指示。");
        template("EMG_GAS", "EMERGENCY: Gas Leak", NoticeCategory.EMERGENCY, Severity.CRITICAL, true,
                "GAS LEAK — EVACUATE, NO FLAMES",
                "A GAS LEAK has been detected near [LOCATION]. Evacuate immediately. Do NOT switch any electrical "
                        + "appliances on or off and do NOT use open flames. Proceed to the assembly area.",
                "氣體洩漏 — 請疏散，切勿生火",
                "[地點] 附近檢測到氣體洩漏。請立即疏散，切勿開關任何電器或生火，並前往集合點。");
    }

    private void template(String code, String name, NoticeCategory cat, Severity sev, boolean emergency,
                          String titleEn, String contentEn, String titleZh, String contentZh) {
        MessageTemplate t = new MessageTemplate();
        t.setCode(code);
        t.setName(name);
        t.setCategory(cat);
        t.setSeverity(sev);
        t.setEmergency(emergency);
        t.setTitleEn(titleEn);
        t.setContentEn(contentEn);
        t.setTitleZh(titleZh);
        t.setContentZh(contentZh);
        templateRepo.save(t);
    }
}
