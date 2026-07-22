package com.smartestate.config;

import com.smartestate.model.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Which notification channels are switched on, driven by {@code app.channels.enabled}
 * (comma-separated, e.g. {@code SMS} or {@code SMS,EMAIL}). Everything else (WhatsApp, Voice, etc.)
 * is treated as disabled: dispatch never uses it, and the resident preferences UI shows it greyed
 * out. Default (property absent) = all four channels enabled.
 */
@Component
public class ChannelPolicy {

    private final Set<Channel> enabled;

    public ChannelPolicy(@Value("${app.channels.enabled:SMS,WHATSAPP,EMAIL,VOICE}") String csv) {
        Set<Channel> set = EnumSet.noneOf(Channel.class);
        for (String part : csv.split(",")) {
            String name = part.trim().toUpperCase();
            if (name.isEmpty()) continue;
            try { set.add(Channel.valueOf(name)); } catch (IllegalArgumentException ignore) { /* skip unknown */ }
        }
        if (set.isEmpty()) set.add(Channel.SMS); // never leave the system with zero channels
        this.enabled = set;
    }

    public boolean isEnabled(Channel channel) {
        return channel != null && enabled.contains(channel);
    }

    public Set<Channel> enabled() {
        return enabled;
    }

    /** The channel to use when a resident's stored/default choice is disabled. */
    public Channel defaultChannel() {
        for (Channel c : Channel.values()) {          // enum order: SMS first
            if (enabled.contains(c)) return c;
        }
        return Channel.SMS;
    }

    /** Enabled channel names (enum order), for injecting into the UI. */
    public List<String> enabledNames() {
        List<String> names = new ArrayList<>();
        for (Channel c : Channel.values()) if (enabled.contains(c)) names.add(c.name());
        return names;
    }
}
