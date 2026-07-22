package com.smartestate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Config-driven AWS notification settings (FR-1.1). NOTHING is hard-coded: region, sender
 * identities and credentials all come from {@code app.aws.messaging.*} (application.properties,
 * env vars, or the Lightsail/EC2 instance profile). When {@code enabled=false} (default) the
 * dispatcher falls back to the built-in mock engine so the app still runs fully offline.
 *
 * Channel -> AWS service mapping:
 *   SMS      -> AWS End User Messaging SMS   (pinpoint-sms-voice-v2 : SendTextMessage)
 *   Voice    -> AWS End User Messaging Voice (pinpoint-sms-voice-v2 : SendVoiceMessage)
 *   Email    -> Amazon SES                   (sesv2 : SendEmail)
 *   WhatsApp -> AWS End User Messaging Social (socialmessaging : SendWhatsAppMessage)
 */
@Component
@ConfigurationProperties(prefix = "app.aws.messaging")
public class AwsMessagingProperties {

    /** Master switch. false -> use the mock/simulated engine (offline). */
    private boolean enabled = false;

    /** AWS region for all messaging clients, e.g. ap-southeast-1. */
    private String region = "ap-southeast-1";

    /** Optional static credentials. Leave BOTH blank to use the default AWS provider chain
     *  (environment variables, ~/.aws profile, or the instance role). */
    private String accessKeyId;
    private String secretAccessKey;

    private final Sms sms = new Sms();
    private final Voice voice = new Voice();
    private final Email email = new Email();
    private final Whatsapp whatsapp = new Whatsapp();

    /** AWS End User Messaging SMS. */
    public static class Sms {
        /** Origination identity: phone-number ARN, phone pool id, or a sender id. */
        private String originationIdentity;
        /** Optional configuration set for event/delivery reporting. */
        private String configurationSet;
        public String getOriginationIdentity() { return originationIdentity; }
        public void setOriginationIdentity(String v) { this.originationIdentity = v; }
        public String getConfigurationSet() { return configurationSet; }
        public void setConfigurationSet(String v) { this.configurationSet = v; }
    }

    /** AWS End User Messaging Voice. */
    public static class Voice {
        private String originationIdentity;   // origination phone number capable of voice
        private String voiceId = "Joanna";    // Amazon Polly voice for text-to-speech
        private String configurationSet;
        public String getOriginationIdentity() { return originationIdentity; }
        public void setOriginationIdentity(String v) { this.originationIdentity = v; }
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String v) { this.voiceId = v; }
        public String getConfigurationSet() { return configurationSet; }
        public void setConfigurationSet(String v) { this.configurationSet = v; }
    }

    /** Amazon SES. */
    public static class Email {
        /** Verified SES sender identity, e.g. "Evergreen Bay Estate <no-reply@your-domain.hk>". */
        private String from;
        private String configurationSet;
        public String getFrom() { return from; }
        public void setFrom(String v) { this.from = v; }
        public String getConfigurationSet() { return configurationSet; }
        public void setConfigurationSet(String v) { this.configurationSet = v; }
    }

    /** AWS End User Messaging Social (WhatsApp Business). */
    public static class Whatsapp {
        /** The WhatsApp origination phone-number id registered in End User Messaging Social. */
        private String originationPhoneNumberId;
        /** Meta Graph API version the payload targets, e.g. v20.0. */
        private String metaApiVersion = "v20.0";
        public String getOriginationPhoneNumberId() { return originationPhoneNumberId; }
        public void setOriginationPhoneNumberId(String v) { this.originationPhoneNumberId = v; }
        public String getMetaApiVersion() { return metaApiVersion; }
        public void setMetaApiVersion(String v) { this.metaApiVersion = v; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAccessKeyId() { return accessKeyId; }
    public void setAccessKeyId(String v) { this.accessKeyId = v; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public void setSecretAccessKey(String v) { this.secretAccessKey = v; }
    public Sms getSms() { return sms; }
    public Voice getVoice() { return voice; }
    public Email getEmail() { return email; }
    public Whatsapp getWhatsapp() { return whatsapp; }
}
