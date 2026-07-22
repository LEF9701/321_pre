package com.smartestate.messaging.aws;

import com.smartestate.config.AwsMessagingProperties;
import com.smartestate.messaging.ChannelSender;
import com.smartestate.messaging.SendOutcome;
import com.smartestate.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

/** Email -> Amazon SES (sesv2 : SendEmail). */
public class EmailSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    private static final String UTF8 = "UTF-8";

    private final SesV2Client client;
    private final AwsMessagingProperties props;

    public EmailSender(SesV2Client client, AwsMessagingProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Channel channel() { return Channel.EMAIL; }

    @Override
    public SendOutcome send(String toAddress, String subject, String body) {
        try {
            Content subj = Content.builder().data(subject == null ? "" : subject).charset(UTF8).build();
            Content text = Content.builder().data(body == null ? "" : body).charset(UTF8).build();
            Message message = Message.builder()
                    .subject(subj)
                    .body(Body.builder().text(text).build())
                    .build();

            SendEmailRequest.Builder req = SendEmailRequest.builder()
                    .fromEmailAddress(props.getEmail().getFrom())
                    .destination(Destination.builder().toAddresses(toAddress).build())
                    .content(EmailContent.builder().simple(message).build());
            if (StringUtils.hasText(props.getEmail().getConfigurationSet())) {
                req.configurationSetName(props.getEmail().getConfigurationSet());
            }
            SendEmailResponse resp = client.sendEmail(req.build());
            return SendOutcome.ok(resp.messageId());
        } catch (Exception e) {
            log.warn("Email send failed to {}: {}", toAddress, e.toString());
            return SendOutcome.fail(e.getMessage());
        }
    }
}
