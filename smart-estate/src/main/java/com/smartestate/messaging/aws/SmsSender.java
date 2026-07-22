package com.smartestate.messaging.aws;

import com.smartestate.config.AwsMessagingProperties;
import com.smartestate.messaging.ChannelSender;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendTextMessageRequest;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendTextMessageResponse;

/** SMS -> AWS End User Messaging SMS (pinpoint-sms-voice-v2 : SendTextMessage). */
public class SmsSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final PinpointSmsVoiceV2Client client;
    private final AwsMessagingProperties props;

    public SmsSender(PinpointSmsVoiceV2Client client, AwsMessagingProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Channel channel() { return Channel.SMS; }

    @Override
    public SendOutcome send(String toAddress, String subject, String body) {
        try {
            SendTextMessageRequest.Builder req = SendTextMessageRequest.builder()
                    .destinationPhoneNumber(toAddress)                       // E.164, e.g. +85291110001
                    .originationIdentity(props.getSms().getOriginationIdentity())
                    .messageBody(body);
            if (StringUtils.hasText(props.getSms().getConfigurationSet())) {
                req.configurationSetName(props.getSms().getConfigurationSet());
            }
            SendTextMessageResponse resp = client.sendTextMessage(req.build());
            return SendOutcome.ok(resp.messageId());
        } catch (Exception e) {
            log.warn("SMS send failed to {}: {}", toAddress, e.toString());
            return SendOutcome.fail(e.getMessage());
        }
    }
}
