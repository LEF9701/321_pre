package com.smartestate.messaging;

import com.smartestate.model.Channel;

/**
 * One outbound gateway for a single {@link Channel}. Each implementation talks to one
 * AWS service. Implementations MUST NOT throw; network/permission errors are returned as
 * {@link SendOutcome#fail}. Only created (as Spring beans) when AWS messaging is enabled, so the
 * absence of a sender for a channel means "no real gateway configured -> use the mock engine".
 */
public interface ChannelSender {

    /** The channel this sender handles. */
    Channel channel();

    /**
     * Send one message.
     * @param toAddress the recipient address for this channel (E.164 phone, or email)
     * @param subject   used by email; ignored by SMS/Voice/WhatsApp
     * @param body      the message body (already localised to the recipient's language)
     */
    SendOutcome send(String toAddress, String subject, String body);

    /**
     * Send a message with full notification context. Channels that need more than a flat title/body
     * (push, which maps severity/category to an iOS interruption level and alert sound) override
     * this; the default just unwraps to {@link #send(String, String, String)} for the AWS senders.
     */
    default SendOutcome send(OutboundMessage message) {
        return send(message.toAddress(), message.subject(), message.body());
    }
}
