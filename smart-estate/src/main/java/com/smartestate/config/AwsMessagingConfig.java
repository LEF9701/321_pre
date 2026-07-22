package com.smartestate.config;

import com.smartestate.messaging.aws.EmailSender;
import com.smartestate.messaging.aws.SmsSender;
import com.smartestate.messaging.aws.VoiceSender;
import com.smartestate.messaging.aws.WhatsAppSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pinpointsmsvoicev2.PinpointSmsVoiceV2Client;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.socialmessaging.SocialMessagingClient;

/**
 * Wires the AWS messaging clients + per-channel senders ONLY when {@code app.aws.messaging.enabled=true}.
 * When disabled, none of these beans exist, so {@link com.smartestate.messaging.MessageDispatcher}
 * has zero senders and the delivery engine keeps its mock behaviour. Region + credentials come
 * entirely from {@link AwsMessagingProperties} (nothing hard-coded); blank static credentials fall
 * back to the default AWS provider chain (env vars / profile / Lightsail-EC2 instance role).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.aws.messaging", name = "enabled", havingValue = "true")
public class AwsMessagingConfig {

    private Region region(AwsMessagingProperties p) {
        return Region.of(p.getRegion());
    }

    private AwsCredentialsProvider credentials(AwsMessagingProperties p) {
        if (StringUtils.hasText(p.getAccessKeyId()) && StringUtils.hasText(p.getSecretAccessKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(p.getAccessKeyId(), p.getSecretAccessKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public PinpointSmsVoiceV2Client pinpointSmsVoiceV2Client(AwsMessagingProperties p) {
        return PinpointSmsVoiceV2Client.builder()
                .region(region(p)).credentialsProvider(credentials(p)).build();
    }

    @Bean
    public SesV2Client sesV2Client(AwsMessagingProperties p) {
        return SesV2Client.builder()
                .region(region(p)).credentialsProvider(credentials(p)).build();
    }

    @Bean
    public SocialMessagingClient socialMessagingClient(AwsMessagingProperties p) {
        return SocialMessagingClient.builder()
                .region(region(p)).credentialsProvider(credentials(p)).build();
    }

    // --- per-channel senders (registered into MessageDispatcher via the ChannelSender list) ---

    @Bean
    public SmsSender smsSender(PinpointSmsVoiceV2Client client, AwsMessagingProperties p) {
        return new SmsSender(client, p);
    }

    @Bean
    public VoiceSender voiceSender(PinpointSmsVoiceV2Client client, AwsMessagingProperties p) {
        return new VoiceSender(client, p);
    }

    @Bean
    public EmailSender emailSender(SesV2Client client, AwsMessagingProperties p) {
        return new EmailSender(client, p);
    }

    @Bean
    public WhatsAppSender whatsAppSender(SocialMessagingClient client, AwsMessagingProperties p) {
        return new WhatsAppSender(client, p);
    }
}
