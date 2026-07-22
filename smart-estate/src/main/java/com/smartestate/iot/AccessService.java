package com.smartestate.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartestate.model.*;
import com.smartestate.repo.AccessCardRepository;
import com.smartestate.repo.AccessEventRepository;
import com.smartestate.repo.AccessPointRepository;
import com.smartestate.repo.CardRegistrationRequestRepository;
import com.smartestate.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Access-control service: authorises a tapped card at a reader, records the movement, and runs the
 * maker-checker workflow for residents editing their own card registration (residents propose,
 * management/admin approve). Movement data is privacy-sensitive: it is only exposed on the
 * management side, and card numbers are masked in views.
 */
@Service
public class AccessService {

    private static final Logger log = LoggerFactory.getLogger(AccessService.class);

    private final AccessCardRepository cardRepo;
    private final AccessPointRepository pointRepo;
    private final AccessEventRepository eventRepo;
    private final CardRegistrationRequestRepository requestRepo;
    private final AuditService auditService;
    private final IotEventBus bus;
    private final ObjectMapper json = new ObjectMapper();

    public AccessService(AccessCardRepository cardRepo, AccessPointRepository pointRepo,
                         AccessEventRepository eventRepo, CardRegistrationRequestRepository requestRepo,
                         AuditService auditService, IotEventBus bus) {
        this.cardRepo = cardRepo;
        this.pointRepo = pointRepo;
        this.eventRepo = eventRepo;
        this.requestRepo = requestRepo;
        this.auditService = auditService;
        this.bus = bus;
    }

    // ---- ingest a card tap (from the MQTT device fleet) --------------------

    @Transactional
    public void ingest(String accessPointCode, String payload) {
        AccessPoint ap = pointRepo.findByCode(accessPointCode).orElse(null);
        if (ap == null) { log.debug("Tap at unknown reader {} ignored.", accessPointCode); return; }

        String cardNumber;
        AccessDirection direction;
        try {
            JsonNode n = json.readTree(payload);
            cardNumber = n.hasNonNull("cardNumber") ? n.get("cardNumber").asText() : "";
            direction = n.hasNonNull("direction") && "OUT".equalsIgnoreCase(n.get("direction").asText())
                    ? AccessDirection.OUT : AccessDirection.IN;
        } catch (Exception e) {
            log.warn("Bad access payload at {}: {}", accessPointCode, e.getMessage());
            return;
        }

        // Octopus opens the common doors (entrance / elevator): any active resident card is admitted
        // estate-wide; an unknown or suspended card is refused.
        AccessCard card = cardRepo.findByCardNumber(cardNumber).orElse(null);
        AccessDecision decision;
        String reason = null;
        if (card == null) {
            decision = AccessDecision.DENIED; reason = "Unknown card";
        } else if (card.getStatus() == CardStatus.SUSPENDED) {
            decision = AccessDecision.DENIED; reason = "Card suspended";
        } else {
            decision = AccessDecision.GRANTED;
        }

        AccessEvent ev = new AccessEvent();
        ev.setCardNumberRaw(cardNumber);
        ev.setCard(card);
        ev.setAccessPoint(ap);
        ev.setDirection(direction);
        ev.setDecision(decision);
        ev.setReason(reason);
        ev.setCreatedAt(LocalDateTime.now());
        eventRepo.save(ev);

        bus.emit("access", view(ev));
    }

    // ---- behaviour log (management side only) -----------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recentEvents(int limit) {
        return eventRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream().map(this::view).toList();
    }

    private Map<String, Object> view(AccessEvent ev) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ev.getId());
        m.put("card", mask(ev.getCardNumberRaw()));
        m.put("resident", ev.getCard() != null && ev.getCard().getOwner() != null
                ? ev.getCard().getOwner().getName() : null);
        m.put("point", ev.getAccessPoint() != null ? ev.getAccessPoint().locationLabel() : null);
        m.put("pointType", ev.getAccessPoint() != null && ev.getAccessPoint().getType() != null
                ? ev.getAccessPoint().getType().name() : null);
        m.put("direction", ev.getDirection() != null ? ev.getDirection().name() : null);
        m.put("decision", ev.getDecision() != null ? ev.getDecision().name() : null);
        m.put("reason", ev.getReason());
        m.put("at", ev.getCreatedAt() != null ? ev.getCreatedAt().toString() : null);
        return m;
    }

    /** Show only the last 4 digits of a card number in any view (movement data is sensitive). */
    private String mask(String number) {
        if (number == null || number.length() <= 4) return "••••";
        return "•••• " + number.substring(number.length() - 4);
    }

    // ---- resident self-service --------------------------------------------

    @Transactional(readOnly = true)
    public AccessCard activeCard(User u) {
        return cardRepo.findFirstByOwnerAndStatusOrderByRegisteredAtDesc(u, CardStatus.ACTIVE).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CardRegistrationRequest> myRequests(User u) {
        return requestRepo.findByRequesterOrderByCreatedAtDesc(u);
    }

    @Transactional
    public void submitRequest(User u, String cardNumber, CardRequestAction action) {
        CardRegistrationRequest r = new CardRegistrationRequest();
        r.setRequester(u);
        r.setCardNumber(action == CardRequestAction.REMOVE ? null : cardNumber);
        r.setAction(action);
        r.setStatus(CardRequestStatus.PENDING);
        r.setCreatedAt(LocalDateTime.now());
        requestRepo.save(r);
        auditService.log("ACCESS_CARD_REQUEST",
                u.getName() + " requested " + action.name() + (cardNumber != null ? " (" + mask(cardNumber) + ")" : ""));
    }

    // ---- management approval (maker-checker) ------------------------------

    @Transactional(readOnly = true)
    public List<CardRegistrationRequest> pendingRequests() {
        return requestRepo.findByStatusOrderByCreatedAtAsc(CardRequestStatus.PENDING);
    }

    @Transactional
    public void approve(Long requestId, String deciderName) {
        CardRegistrationRequest r = requestRepo.findById(requestId).orElseThrow();
        if (r.getStatus() != CardRequestStatus.PENDING) return;
        User requester = r.getRequester();

        if (r.getAction() == CardRequestAction.REMOVE) {
            suspendActiveCards(requester);
        } else {
            // ADD or REPLACE: retire any current card, then register the new one to this resident.
            suspendActiveCards(requester);
            AccessCard card = cardRepo.findByCardNumber(r.getCardNumber()).orElseGet(AccessCard::new);
            card.setCardNumber(r.getCardNumber());
            card.setOwner(requester);
            card.setStatus(CardStatus.ACTIVE);
            card.setRegisteredAt(LocalDateTime.now());
            cardRepo.save(card);
        }
        r.setStatus(CardRequestStatus.APPROVED);
        r.setDecidedByName(deciderName);
        r.setDecidedAt(LocalDateTime.now());
        requestRepo.save(r);
        auditService.log("ACCESS_CARD_APPROVE",
                deciderName + " approved " + r.getAction().name() + " for " + requester.getName());
    }

    @Transactional
    public void reject(Long requestId, String deciderName) {
        CardRegistrationRequest r = requestRepo.findById(requestId).orElseThrow();
        if (r.getStatus() != CardRequestStatus.PENDING) return;
        r.setStatus(CardRequestStatus.REJECTED);
        r.setDecidedByName(deciderName);
        r.setDecidedAt(LocalDateTime.now());
        requestRepo.save(r);
        auditService.log("ACCESS_CARD_REJECT",
                deciderName + " rejected " + r.getAction().name() + " for " + r.getRequester().getName());
    }

    private void suspendActiveCards(User u) {
        for (AccessCard c : cardRepo.findByOwnerOrderByRegisteredAtDesc(u)) {
            if (c.getStatus() == CardStatus.ACTIVE) {
                c.setStatus(CardStatus.SUSPENDED);
                cardRepo.save(c);
            }
        }
    }
}
