package com.smartestate.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.smartestate.messaging.push.PushSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Wires the APNs client + {@link PushSender} ONLY when {@code app.push.apns.enabled=true} (mirrors
 * {@link AwsMessagingConfig}). When disabled, neither bean exists, so
 * {@link com.smartestate.messaging.MessageDispatcher} has no PUSH sender and the delivery engine
 * keeps its mock behaviour (the app runs offline with simulated push). Key, ids and environment
 * come from {@link ApnsProperties}; nothing is hard-coded.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.push.apns", name = "enabled", havingValue = "true")
public class PushConfig {

    @Bean(destroyMethod = "close")
    public ApnsClient apnsClient(ApnsProperties p)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        String host = "PRODUCTION".equalsIgnoreCase(p.getEnvironment())
                ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
        return new ApnsClientBuilder()
                .setApnsServer(host)
                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(
                        new File(p.getP8KeyPath()), p.getTeamId(), p.getKeyId()))
                .build();
    }

    @Bean
    public PushSender pushSender(ApnsClient apnsClient, ApnsProperties p) {
        return new PushSender(apnsClient, p);
    }
}
