package com.smartestate.messaging;

import com.smartestate.model.Channel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The single entry point the delivery engine calls to send a message. It selects the right
 * {@link ChannelSender} for the {@link Channel} at runtime. When AWS messaging is disabled no
 * senders exist, so {@link #supports} returns false and the caller keeps the mock behaviour.
 * This bean always exists (even with zero senders), so callers can inject it unconditionally.
 */
@Component
public class MessageDispatcher {

    private final Map<Channel, ChannelSender> senders = new EnumMap<>(Channel.class);

    public MessageDispatcher(List<ChannelSender> channelSenders) {
        for (ChannelSender s : channelSenders) {
            senders.put(s.channel(), s); // one sender per channel
        }
    }

    /** Is a real AWS gateway wired up for this channel? */
    public boolean supports(Channel channel) {
        return senders.containsKey(channel);
    }

    /** Route to the correct AWS service. Precondition: {@link #supports(Channel)} is true. */
    public SendOutcome send(Channel channel, String toAddress, String subject, String body) {
        ChannelSender sender = senders.get(channel);
        if (sender == null) {
            return SendOutcome.fail("No gateway configured for channel " + channel);
        }
        return sender.send(toAddress, subject, body);
    }

    /** Route a context-rich message (used by push) to the correct sender. */
    public SendOutcome send(Channel channel, OutboundMessage message) {
        ChannelSender sender = senders.get(channel);
        if (sender == null) {
            return SendOutcome.fail("No gateway configured for channel " + channel);
        }
        return sender.send(message);
    }
}
