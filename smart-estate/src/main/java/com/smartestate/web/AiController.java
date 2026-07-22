package com.smartestate.web;

import com.smartestate.ai.AiEvaluationService;
import com.smartestate.ai.ChatTurn;
import com.smartestate.ai.ClaudeClient;
import com.smartestate.ai.IssueClassifier;
import com.smartestate.ai.RagService;
import com.smartestate.model.BylawDoc;
import com.smartestate.model.Language;
import com.smartestate.repo.BylawDocRepository;
import com.smartestate.service.AuditService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ai")
public class AiController {

    private final RagService ragService;
    private final IssueClassifier issueClassifier;
    private final AiEvaluationService evaluationService;
    private final BylawDocRepository bylawRepo;
    private final ClaudeClient claudeClient;
    private final AuditService auditService;

    public AiController(RagService ragService, IssueClassifier issueClassifier,
                        AiEvaluationService evaluationService, BylawDocRepository bylawRepo,
                        ClaudeClient claudeClient, AuditService auditService) {
        this.ragService = ragService;
        this.issueClassifier = issueClassifier;
        this.evaluationService = evaluationService;
        this.bylawRepo = bylawRepo;
        this.claudeClient = claudeClient;
        this.auditService = auditService;
    }

    // ---- FR-8.1 chatbot ---------------------------------------------------

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("claudeAvailable", claudeClient.isAvailable());
        return "ai/chat";
    }

    /** No-JS fallback: plain form post that re-renders the page with a single answer. */
    @PostMapping("/chat")
    public String ask(@RequestParam String question, Model model) {
        model.addAttribute("claudeAvailable", claudeClient.isAvailable());
        model.addAttribute("question", question);
        model.addAttribute("result", ragService.ask(question));
        auditService.log("AI_CHAT", "Bylaw query: " + question);
        return "ai/chat";
    }

    /**
     * Same retrieval + answer as the form post above, returned as JSON so the chat page can hold a
     * running conversation without a full reload. The response is the unchanged
     * {@link RagService.RagAnswer}; only the request carries the extra conversation history.
     */
    @PostMapping(value = "/chat/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RagService.RagAnswer askJson(@RequestBody AskRequest req) {
        String question = req.getQuestion() == null ? "" : req.getQuestion();
        auditService.log("AI_CHAT", "Bylaw query: " + question);
        // Only the question is logged, never the answer; answers may now carry account figures.
        return ragService.ask(question, req.toHistory());
    }

    /** Back-compat form variant, so a stale JS bundle keeps working against a new JAR. */
    @PostMapping(value = "/chat/ask", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public RagService.RagAnswer askForm(@RequestParam String question) {
        auditService.log("AI_CHAT", "Bylaw query: " + question);
        return ragService.ask(question);
    }

    /** Request body for the conversational endpoint. History is untrusted client input. */
    public static class AskRequest {
        private String question;
        private List<Turn> history = new ArrayList<>();

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public List<Turn> getHistory() { return history; }
        public void setHistory(List<Turn> history) { this.history = history == null ? new ArrayList<>() : history; }

        List<ChatTurn> toHistory() {
            List<ChatTurn> turns = new ArrayList<>();
            for (Turn t : history) {
                if (t == null || t.getText() == null || t.getText().isBlank()) continue;
                turns.add("ASSISTANT".equalsIgnoreCase(t.getRole())
                        ? ChatTurn.assistant(t.getText())
                        : ChatTurn.user(t.getText()));
            }
            return turns;
        }

        public static class Turn {
            private String role;
            private String text;
            public String getRole() { return role; }
            public void setRole(String role) { this.role = role; }
            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }
    }

    // ---- FR-8.2 document ingestion (admin) --------------------------------

    @GetMapping("/documents")
    public String documents(Model model) {
        model.addAttribute("docs", bylawRepo.findAllByOrderByCodeAsc());
        model.addAttribute("languages", Language.values());
        return "ai/documents";
    }

    @PostMapping("/documents")
    public String ingest(@RequestParam(required = false) String code, @RequestParam String title,
                         @RequestParam Language language, @RequestParam(required = false) String category,
                         @RequestParam String content, RedirectAttributes ra) {
        BylawDoc doc = new BylawDoc();
        doc.setCode(code == null || code.isBlank() ? "DOC-" + (bylawRepo.count() + 1) : code.trim());
        doc.setTitle(title);
        doc.setLanguage(language);
        doc.setCategory(category);
        doc.setContent(content);
        bylawRepo.save(doc);
        ragService.rebuild();
        auditService.log("AI_INGEST", "Indexed bylaw doc " + doc.getCode() + " (" + title + ")");
        Flash.msg(ra, "aiDoc.ingested",
                "Document \"" + title + "\" ingested and indexed. The chatbot can now use it.", title);
        return "redirect:/ai/documents";
    }

    /** FR-8.2: edit an existing document and re-index it so the chatbot uses the new text. */
    @PostMapping("/documents/{id}")
    public String updateDoc(@PathVariable Long id, @RequestParam String title,
                            @RequestParam Language language, @RequestParam(required = false) String category,
                            @RequestParam String content, RedirectAttributes ra) {
        bylawRepo.findById(id).ifPresent(d -> {
            d.setTitle(title);
            d.setLanguage(language);
            d.setCategory(category);
            d.setContent(content);
            bylawRepo.save(d);
            ragService.rebuild();
            auditService.log("AI_DOC_EDIT", "Edited & re-indexed bylaw doc " + d.getCode());
        });
        Flash.msg(ra, "aiDoc.updated", "Document updated and re-indexed. The chatbot now uses the new text.");
        return "redirect:/ai/documents";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteDoc(@PathVariable Long id, RedirectAttributes ra) {
        bylawRepo.findById(id).ifPresent(d -> {
            bylawRepo.delete(d);
            ragService.rebuild();
            auditService.log("AI_DOC_DELETE", "Removed bylaw doc " + d.getCode());
        });
        Flash.msg(ra, "aiDoc.removed", "Document removed and index rebuilt.");
        return "redirect:/ai/documents";
    }

    // ---- FR-8.3 evaluation (rp.pdf §5) ------------------------------------

    @GetMapping("/evaluation")
    public String evaluation(Model model) {
        // ensure the classifier index reflects current issues
        issueClassifier.rebuild();
        model.addAttribute("classification", evaluationService.evaluateClassification());
        model.addAttribute("retrieval", evaluationService.evaluateRetrieval());
        model.addAttribute("followups", evaluationService.evaluateFollowups());
        model.addAttribute("bilingual", evaluationService.evaluateBilingual());
        return "ai/evaluation";
    }
}
