package com.smartestate.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Redirect flash messages, carried as a message code plus arguments so the React
 * side can render them in the reader's language (see frontend/src/lib/flash.js).
 * The English sentence travels alongside the code as {@code msg}/{@code error}; the
 * UI falls back to it for any unrecognised code, so a new message never shows a blank alert.
 * Enum arguments are passed as the enum NAME (e.g. {@code type.name()}), not the English
 * label; the UI resolves it to a localised label of its own.
 */
public final class Flash {

    private Flash() {}

    /** Success message. */
    public static void msg(RedirectAttributes ra, String code, String english, Object... args) {
        ra.addFlashAttribute("msg", english);
        ra.addFlashAttribute("msgCode", code);
        ra.addFlashAttribute("msgArgs", args(args));
    }

    /** Failure message. */
    public static void error(RedirectAttributes ra, String code, String english, Object... args) {
        ra.addFlashAttribute("error", english);
        ra.addFlashAttribute("errorCode", code);
        ra.addFlashAttribute("errorArgs", args(args));
    }

    // Arrays.asList (not List.of) so a null argument degrades to an empty slot
    // rather than throwing while building a user-facing message.
    private static List<Object> args(Object... args) {
        return args == null ? List.of() : Arrays.asList(args);
    }
}
