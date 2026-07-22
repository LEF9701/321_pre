package com.smartestate.messaging.aws;

import com.smartestate.config.AwsMessagingProperties;
import com.smartestate.messaging.ChannelSender;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.socialmessaging.SocialMessagingClient;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageRequest;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageResponse;

/**
 * WhatsApp -> AWS End User Messaging Social (socialmessaging : SendWhatsAppMessage).
 * The {@code message} is the raw WhatsApp Cloud API JSON payload, passed through by AWS to Meta.
 *
 * NOTE: this sends a free-form "text" message, which Meta only accepts inside an open 24-hour
 * customer-service window. For business-initiated notifications you must send a pre-approved
 * "template" message instead. That is a change to the JSON payload built below (swap
 * type/text for type/template), not to the AWS call.
 */
public class WhatsAppSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);

    private final SocialMessagingClient client;
    private final AwsMessagingProperties props;

    public WhatsAppSender(SocialMessagingClient client, AwsMessagingProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Channel channel() { return Channel.WHATSAPP; }

    @Override
    public SendOutcome send(String toAddress, String subject, String body) {
        try {
            String to = toAddress == null ? "" : toAddress.replaceAll("[^0-9]", ""); // digits only, no '+'
            String payload = "{\"messaging_product\":\"whatsapp\","
                    + "\"to\":\"" + to + "\","
                    + "\"type\":\"text\","
                    + "\"text\":{\"preview_url\":false,\"body\":\"" + jsonEscape(body) + "\"}}";

            SendWhatsAppMessageRequest req = SendWhatsAppMessageRequest.builder()
                    .originationPhoneNumberId(props.getWhatsapp().getOriginationPhoneNumberId())
                    .metaApiVersion(props.getWhatsapp().getMetaApiVersion())
                    .message(SdkBytes.fromUtf8String(payload))
                    .build();
            SendWhatsAppMessageResponse resp = client.sendWhatsAppMessage(req);
            return SendOutcome.ok(resp.messageId());
        } catch (Exception e) {
            log.warn("WhatsApp send failed to {}: {}", toAddress, e.toString());
            return SendOutcome.fail(e.getMessage());
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default   -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.toString();
    }
}
