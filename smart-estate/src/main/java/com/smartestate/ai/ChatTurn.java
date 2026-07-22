package com.smartestate.ai;

/**
 * One turn of the FR-8.1 chat conversation, supplied by the client so the assistant can resolve
 * follow-up questions ("What about on Sundays?") against what was already asked.
 *
 * <p>This is an <em>input</em> type only; the answer model ({@link RagService.RagAnswer}) is
 * unchanged. Client-supplied history is untrusted: it may steer phrasing, but it never selects
 * whose data is read (account facts are always resolved server-side from the security context).
 */
public record ChatTurn(Role role, String text) {

    public enum Role { USER, ASSISTANT }

    public static ChatTurn user(String text) { return new ChatTurn(Role.USER, text); }

    public static ChatTurn assistant(String text) { return new ChatTurn(Role.ASSISTANT, text); }

    public boolean isUser() { return role == Role.USER; }
}
