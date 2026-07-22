package com.smartestate.messaging.aws;

import com.smartestate.config.AwsMessagingProperties;
import com.smartestate.messaging.ChannelSender;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendVoiceMessageRequest;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.SendVoiceMessageResponse;
import software.amazon.awssdk.services.pinpointsmsvoicev2.model.VoiceMessageBodyTextType;

/** Voice -> AWS End User Messaging Voice (pinpoint-sms-voice-v2 : SendVoiceMessage, text-to-speech). */
public class VoiceSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(VoiceSender.class);

    private final PinpointSmsVoiceV2Client client;
    private final AwsMessagingProperties props;

    public VoiceSender(PinpointSmsVoiceV2Client client, AwsMessagingProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Channel channel() { return Channel.VOICE; }

    @Override
    public SendOutcome send(String toAddress, String subject, String body) {
        try {
            SendVoiceMessageRequest.Builder req = SendVoiceMessageRequest.builder()
                    .destinationPhoneNumber(toAddress)
                    .originationIdentity(props.getVoice().getOriginationIdentity())
                    .messageBody(body)                                       // spoken via Amazon Polly
                    .messageBodyTextType(VoiceMessageBodyTextType.TEXT);
            if (StringUtils.hasText(props.getVoice().getVoiceId())) {
                req.voiceId(props.getVoice().getVoiceId());
            }
            if (StringUtils.hasText(props.getVoice().getConfigurationSet())) {
                req.configurationSetName(props.getVoice().getConfigurationSet());
            }
            SendVoiceMessageResponse resp = client.sendVoiceMessage(req.build());
            return SendOutcome.ok(resp.messageId());
        } catch (Exception e) {
            log.warn("Voice call failed to {}: {}", toAddress, e.toString());
            return SendOutcome.fail(e.getMessage());
        }
    }
}
