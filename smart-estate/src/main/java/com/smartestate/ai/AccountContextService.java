package com.smartestate.ai;

import com.smartestate.model.Issue;
import com.smartestate.model.IssueStatus;
import com.smartestate.model.Role;
import com.smartestate.model.Unit;
import com.smartestate.model.User;
import com.smartestate.service.CurrentUserService;
import com.smartestate.service.IssueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

/**
 * Builds the "your account" grounding block so the assistant can answer questions residents
 * actually care about ("is my fee overdue?") instead of only quoting bylaws.
 *
 * <p>Authorization: the signed-in user is resolved server-side from the security context; only
 * their <em>own</em> unit and reported issues are read. Nothing in the chat request can select
 * whose data is fetched. Management and admin get role only, with no cross-resident access.
 */
@Service
public class AccountContextService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    /**
     * Cue words that make the resident's own record relevant.
     *
     * <p>Narrow: domain terms only, no bare possessives. "my" alone matches things like
     * "water is leaking into my bathroom", which would attach the resident's fee balance to a question
     * that has nothing to do with it. Personal data should reach the model only when the question is
     * actually about the resident's account or cases.
     */
    private static final List<String> ACCOUNT_CUES = List.of(
            "fee", "balance", "overdue", "outstanding", "arrear", "past due", "due date",
            "payment", "pay", "owe", "invoice", "surcharge", "late charge",
            // Not "my unit"/"my flat": residents phrase ordinary bylaw questions that way
            // ("can I keep a dog in my flat?"), which is not a question about their record.
            "my case", "my issue", "my report", "my ticket", "my repair",
            "case status", "issue status", "ref number", "reference number",
            "管理費", "結餘", "逾期", "欠費", "欠款", "繳費", "附加費",
            "我的個案", "我的報修", "報修進度", "個案進度");

    private final CurrentUserService currentUser;
    private final IssueService issueService;

    @Value("${app.ai.account-context.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.account-context.max-issues:5}")
    private int maxIssues;

    public AccountContextService(CurrentUserService currentUser, IssueService issueService) {
        this.currentUser = currentUser;
        this.issueService = issueService;
    }

    /**
     * Only build the block when the question actually concerns the resident's own record. A
     * greeting or a generic bylaw lookup should never ship personal data to the model.
     */
    public boolean isRelevant(String query) {
        if (!enabled || query == null || query.isBlank()) return false;
        String q = query.toLowerCase(Locale.ROOT);
        return ACCOUNT_CUES.stream().anyMatch(q::contains);
    }

    /** The signed-in user's own account facts as a prompt block, or null when there is nothing to say. */
    public String forCurrentUser() {
        if (!enabled) return null;
        User u = currentUser.get();
        if (u == null) return null;

        if (u.getRole() != Role.RESIDENT) {
            // Staff have no unit of their own; say so rather than silently omitting the block.
            return "Signed-in user: " + u.getName() + " (" + u.getRole() + ", management staff). "
                    + "No personal unit or resident record is attached to this account.";
        }

        StringBuilder sb = new StringBuilder("Signed-in resident: ").append(u.getName()).append(".\n");
        Unit unit = u.getUnit();
        if (unit == null) {
            sb.append("No unit is linked to this account yet.\n");
        } else {
            sb.append("Unit: ").append(unit.getFullCode())
                    .append(" · management fee status: ").append(unit.getFeeStatus())
                    .append(" · outstanding balance: HK$ ")
                    .append(MONEY.format(unit.getBalance() == null ? 0 : unit.getBalance()));
            if (unit.getDueDate() != null) sb.append(" · due ").append(unit.getDueDate());
            sb.append(".\n");
        }

        List<Issue> open = issueService.forReporter(u).stream()
                .filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.WITHDRAWN)
                .limit(maxIssues)
                .toList();
        if (open.isEmpty()) {
            sb.append("Open maintenance issues reported by this resident: none.\n");
        } else {
            sb.append("Open maintenance issues reported by this resident:\n");
            for (Issue i : open) {
                sb.append("  - ").append(i.getRefNumber())
                        .append(" · ").append(i.getCategory())
                        .append(" · ").append(i.getStatus());
                if (i.getAssignedTo() != null && !i.getAssignedTo().isBlank()) {
                    sb.append(" · assigned to ").append(i.getAssignedTo());
                }
                if (i.getCreatedAt() != null) sb.append(" · raised ").append(i.getCreatedAt().toLocalDate());
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** Short title for the citation entry, so the resident can see which record was used. */
    public String sourceTitle() {
        User u = currentUser.get();
        if (u != null && u.getRole() == Role.RESIDENT && u.getUnit() != null) {
            return "Your account · " + u.getUnit().getFullCode();
        }
        return "Your account";
    }
}
