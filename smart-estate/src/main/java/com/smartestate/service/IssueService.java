package com.smartestate.service;

import com.smartestate.model.*;
import com.smartestate.repo.IssueCommentRepository;
import com.smartestate.repo.IssueRepository;
import com.smartestate.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final AuditService auditService;

    public IssueService(IssueRepository issueRepository,
                        IssueCommentRepository commentRepository,
                        NotificationRepository notificationRepository,
                        NotificationDispatchService dispatchService,
                        AuditService auditService) {
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.dispatchService = dispatchService;
        this.auditService = auditService;
    }

    /** FR-3.3 / FR-5.1 / FR-5.2: create an issue and assign an auto-generated tracking number. */
    @Transactional
    public Issue create(User reporter, IssueCategory category, String description, String photoFilename) {
        Issue issue = new Issue();
        issue.setReporter(reporter);
        issue.setUnit(reporter != null ? reporter.getUnit() : null);
        issue.setCategory(category);
        issue.setDescription(description);
        issue.setPhotoFilename(photoFilename);
        issue.setPriority(IssuePriority.MEDIUM);
        issue.setStatus(IssueStatus.SUBMITTED);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setUpdatedAt(LocalDateTime.now());
        issue = issueRepository.save(issue);
        issue.setRefNumber(String.format("RPT-%04d", issue.getId()));
        issue = issueRepository.save(issue);
        auditService.log("ISSUE_CREATE", issue.getRefNumber() + " (" + category + ")");
        return issue;
    }

    /** FR-5.3 status workflow + FR-5.5 notify resident on status change. */
    @Transactional
    public void updateStatus(Issue issue, IssueStatus newStatus, String note) {
        IssueStatus old = issue.getStatus();
        issue.setStatus(newStatus);
        issue.setUpdatedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) issue.setResolutionNote(note);
        issueRepository.save(issue);
        auditService.log("ISSUE_STATUS", issue.getRefNumber() + ": " + old + " -> " + newStatus);
        notifyReporter(issue, old, newStatus);
    }

    @Transactional
    public void updatePriority(Issue issue, IssuePriority priority) {
        issue.setPriority(priority);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);
        auditService.log("ISSUE_PRIORITY", issue.getRefNumber() + " -> " + priority);
    }

    @Transactional
    public void assign(Issue issue, String assignee) {
        issue.setAssignedTo(assignee);
        issue.setUpdatedAt(LocalDateTime.now());
        if (issue.getStatus() == IssueStatus.SUBMITTED) {
            issue.setStatus(IssueStatus.UNDER_REVIEW);
        }
        issueRepository.save(issue);
        auditService.log("ISSUE_ASSIGN", issue.getRefNumber() + " -> " + assignee);
    }

    /** FR-5.5 (Should): push a status-change update to the resident via the dispatch engine. */
    private void notifyReporter(Issue issue, IssueStatus oldStatus, IssueStatus newStatus) {
        User reporter = issue.getReporter();
        if (reporter == null) return;

        Notification n = new Notification();
        n.setTitle("Issue " + issue.getRefNumber() + " is now " + newStatus.getLabel());
        n.setTitleZh("維修申報 " + issue.getRefNumber() + " 狀態更新為「" + newStatus.getLabelZh() + "」");
        n.setCategory(NoticeCategory.MAINTENANCE);
        n.setSeverity(Severity.INFO);
        n.setCreatedByName("Management (auto)");
        n.setCreatedAt(LocalDateTime.now());
        n.setRecipientScope("Reporter of " + issue.getRefNumber());
        String body = "Your reported issue " + issue.getRefNumber() + " (" + issue.getCategory().getLabel()
                + ") changed from " + oldStatus.getLabel() + " to " + newStatus.getLabel() + ".";
        String bodyZh = "你申報的維修個案 " + issue.getRefNumber() + "（" + issue.getCategory().getLabelZh()
                + "）狀態已由「" + oldStatus.getLabelZh() + "」更新為「" + newStatus.getLabelZh() + "」。";
        if (issue.getResolutionNote() != null && !issue.getResolutionNote().isBlank()) {
            body += " Note: " + issue.getResolutionNote();
            bodyZh += "備註：" + issue.getResolutionNote();
        }
        n.setContentEn(body);
        n.setContentZh(bodyZh);
        notificationRepository.save(n);
        dispatchService.dispatch(n, List.of(reporter), false);
    }

    /** A resident may withdraw their own issue while it is still just SUBMITTED. */
    @Transactional
    public void withdraw(Issue issue) {
        issue.setStatus(IssueStatus.WITHDRAWN);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);
        auditService.log("ISSUE_WITHDRAW", issue.getRefNumber() + " withdrawn by resident");
    }

    /** Two-way comment thread between the resident and management. */
    @Transactional
    public IssueComment addComment(Issue issue, User author, String body) {
        IssueComment c = new IssueComment();
        c.setIssue(issue);
        c.setAuthorName(author != null ? author.getName() : "Unknown");
        c.setAuthorRole(author != null ? author.getRole() : Role.RESIDENT);
        c.setBody(body);
        c.setCreatedAt(LocalDateTime.now());
        c = commentRepository.save(c);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);
        auditService.log("ISSUE_COMMENT", issue.getRefNumber() + " commented by " + c.getAuthorName());
        // When management replies, push a notice to the resident so they see it on their channels.
        if (author != null && author.getRole() != Role.RESIDENT) {
            notifyReporterComment(issue, c);
        }
        return c;
    }

    public List<IssueComment> comments(Issue issue) {
        return commentRepository.findByIssueOrderByCreatedAtAsc(issue);
    }

    private void notifyReporterComment(Issue issue, IssueComment c) {
        User reporter = issue.getReporter();
        if (reporter == null) return;
        Notification n = new Notification();
        n.setTitle("New reply on issue " + issue.getRefNumber());
        n.setTitleZh("維修申報 " + issue.getRefNumber() + " 有新回覆");
        n.setCategory(NoticeCategory.MAINTENANCE);
        n.setSeverity(Severity.INFO);
        n.setCreatedByName(c.getAuthorName());
        n.setCreatedAt(LocalDateTime.now());
        n.setRecipientScope("Reporter of " + issue.getRefNumber());
        n.setContentEn("Management replied on your issue " + issue.getRefNumber() + ": " + c.getBody());
        n.setContentZh("管理處就你申報的維修個案 " + issue.getRefNumber() + " 作出回覆：" + c.getBody());
        notificationRepository.save(n);
        dispatchService.dispatch(n, List.of(reporter), false);
    }

    public List<Issue> all() { return issueRepository.findAllByOrderByCreatedAtDesc(); }
    public List<Issue> forReporter(User u) { return issueRepository.findByReporterOrderByCreatedAtDesc(u); }
}
