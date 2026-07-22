package com.smartestate.seed;

import com.smartestate.model.*;
import com.smartestate.repo.AccessCardRepository;
import com.smartestate.repo.AccessEventRepository;
import com.smartestate.repo.AccessPointRepository;
import com.smartestate.repo.BlockRepository;
import com.smartestate.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds Octopus cards + access points + a history of entrance taps so the access-control and
 * entrance-log pages are populated on first load, and so the simulator's cards/readers line up with
 * real residents. An estate card admits its holder at any tower's common entrance/lift (estate-wide
 * access); a flat door admits only that unit's resident. Runs after {@link DataSeeder}.
 */
@Component
@Order(3)
public class AccessSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final AccessCardRepository cardRepo;
    private final AccessPointRepository pointRepo;
    private final AccessEventRepository eventRepo;
    private final BlockRepository blockRepo;

    /** Fixed card numbers for the named demo residents, mirrored in the simulator console.
     *  Some flats have two card-holders (household), e.g. 5/F-03 Chan Tai Man + Chan Siu Ling. */
    private static final Map<String, String> NAMED_CARDS = Map.ofEntries(
            Map.entry("resident@smartestate.hk", "88220011"),   // Chan Tai Man   · Azalea 5/F-03
            Map.entry("chan.sl@smartestate.hk",  "88220012"),   // Chan Siu Ling  · Azalea 5/F-03 (same flat)
            Map.entry("wong@smartestate.hk",     "77330022"),   // Wong Mei Ling  · Azalea 5/F-04
            Map.entry("lee@smartestate.hk",      "66550033"),   // Lee Chi Wai    · Azalea 3/F-02
            Map.entry("lee.ky@smartestate.hk",   "66550034"),   // Lee Ka Yiu     · Azalea 3/F-02 (same flat)
            Map.entry("cheung@smartestate.hk",   "55660044"),   // Cheung Ka Yan  · Azalea 6/F-01
            Map.entry("ng@smartestate.hk",       "44770055"),   // Ng Ho Yin      · Azalea 2/F-07
            Map.entry("lam@smartestate.hk",      "33880066"),   // Lam Siu Fong   · Azalea 4/F-05
            Map.entry("lam.wk@smartestate.hk",   "33880067"));  // Lam Wai Kit    · Azalea 4/F-05 (same flat)

    public AccessSeeder(UserRepository userRepo, AccessCardRepository cardRepo,
                        AccessPointRepository pointRepo, AccessEventRepository eventRepo,
                        BlockRepository blockRepo) {
        this.userRepo = userRepo;
        this.cardRepo = cardRepo;
        this.pointRepo = pointRepo;
        this.eventRepo = eventRepo;
        this.blockRepo = blockRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (pointRepo.count() > 0) return;
        List<Block> blocks = blockRepo.findAll();
        if (blocks.isEmpty()) return;

        // Octopus readers at each tower's common doors (entrance + lift).
        Map<Long, AccessPoint> entranceByBlock = new HashMap<>();
        for (Block block : blocks) {
            String ab = abbr(block);
            entranceByBlock.put(block.getId(),
                    point("AP-" + ab + "-ENT", AccessPointType.BUILDING_ENTRANCE, block, null, "Main entrance"));
            point("AP-" + ab + "-LIFT", AccessPointType.ELEVATOR, block, null, "Elevator");
        }

        // Every resident gets their OWN card, so a household of two/three each has door + lift access.
        List<AccessCard> cards = new ArrayList<>();
        int seq = 0;
        for (User u : userRepo.findByRole(Role.RESIDENT)) {
            if (u.getUnit() == null || u.getUnit().getFloor() == null) continue;
            String number = NAMED_CARDS.getOrDefault(u.getEmail(), String.format("9%07d", seq++));
            cards.add(card(number, u));
        }

        seedEntranceHistory(cards, entranceByBlock);
    }

    /** A day's worth of entrance taps so the access log has content immediately (bounded so the log
     *  stays a readable demo size regardless of how many residents were seeded). */
    private void seedEntranceHistory(List<AccessCard> cards, Map<Long, AccessPoint> entranceByBlock) {
        Random rnd = new Random(11);   // deterministic history
        LocalDateTime now = LocalDateTime.now();
        int budget = 120;              // total seeded events cap

        for (AccessCard c : cards) {
            if (budget <= 0) break;
            Block block = c.getOwner().getUnit().getFloor().getBlock();
            AccessPoint entrance = entranceByBlock.get(block.getId());
            if (entrance == null) continue;
            int entries = 1 + rnd.nextInt(2);   // 1..2 entries per resident today
            for (int i = 0; i < entries && budget > 0; i++, budget--) {
                event(c, c.getCardNumber(), entrance, AccessDirection.IN, AccessDecision.GRANTED, null,
                        now.minusMinutes(15 + rnd.nextInt(600)));
            }
        }
        // a few denied taps by an unknown/visitor card at random entrances
        List<AccessPoint> entrances = new ArrayList<>(entranceByBlock.values());
        for (int k = 0; k < 3 && !entrances.isEmpty(); k++) {
            AccessPoint e = entrances.get(rnd.nextInt(entrances.size()));
            event(null, "00009999", e, AccessDirection.IN, AccessDecision.DENIED, "Unknown card",
                    now.minusMinutes(20 + rnd.nextInt(600)));
        }
    }

    private void event(AccessCard card, String number, AccessPoint point, AccessDirection dir,
                       AccessDecision decision, String reason, LocalDateTime at) {
        AccessEvent ev = new AccessEvent();
        ev.setCard(card);
        ev.setCardNumberRaw(number);
        ev.setAccessPoint(point);
        ev.setDirection(dir);
        ev.setDecision(decision);
        ev.setReason(reason);
        ev.setCreatedAt(at);
        eventRepo.save(ev);
    }

    private AccessPoint point(String code, AccessPointType type, Block block, Unit unit, String label) {
        AccessPoint p = new AccessPoint();
        p.setCode(code);
        p.setType(type);
        p.setBlock(block);
        p.setUnit(unit);
        p.setLabel(label);
        return pointRepo.save(p);
    }

    private AccessCard card(String number, User owner) {
        AccessCard c = new AccessCard();
        c.setCardNumber(number);
        c.setOwner(owner);
        c.setStatus(CardStatus.ACTIVE);
        c.setRegisteredAt(LocalDateTime.now());
        return cardRepo.save(c);
    }

    private String abbr(Block b) {
        String name = b.getBlockName() == null ? "BLK" : b.getBlockName().trim();
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
    }
}
