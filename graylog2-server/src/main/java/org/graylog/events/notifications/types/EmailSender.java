/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.notifications.types;

import com.floreysoft.jmte.Engine;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.email.EmailFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

public class EmailSender {
    private static final Logger LOG = LoggerFactory.getLogger(EmailSender.class);

    private final NotificationService notificationService;
    private final NodeId nodeId;
    private final Engine templateEngine;
    private final Engine htmlTemplateEngine;
    private final EmailFactory emailFactory;

    @Inject
    public EmailSender(NotificationService notificationService,
                       NodeId nodeId,
                       Engine templateEngine,
                       @Named("HtmlSafe") Engine htmlTemplateEngine,
                       EmailFactory emailFactory) {
        this.notificationService = requireNonNull(notificationService, "notificationService");
        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
        this.htmlTemplateEngine = requireNonNull(htmlTemplateEngine, "htmlTemplateEngine");
        this.emailFactory = requireNonNull(emailFactory, "emailFactory");
    }

    @VisibleForTesting
    private String buildSubject(EmailEventNotificationConfig config, Map<String, Object> model) {
        final String template;
        if (isNullOrEmpty(config.subject())) {
            template = EmailEventNotificationConfig.DEFAULT_SUBJECT;
        } else {
            template = config.subject();
        }

        return templateEngine.transform(template, model);
    }

    @VisibleForTesting
    private String buildBody(EmailEventNotificationConfig config, Map<String, Object> model) {
        final String template;
        if (isNullOrEmpty(config.bodyTemplate())) {
            template = EmailEventNotificationConfig.DEFAULT_BODY_TEMPLATE;
        } else {
            template = config.bodyTemplate();
        }

        return templateEngine.transform(template, model);
    }

    @VisibleForTesting
    private String buildHtmlBody(EmailEventNotificationConfig config, Map<String, Object> model) {
        return htmlTemplateEngine.transform(config.htmlBodyTemplate(), model);
    }

    private void sendEmails(EmailEventNotificationConfig config, Set<String> recipients, Set<String> ccEmails,
                            Set<String> bccEmails, String sender, String replyTo, Map<String, Object> model,
                            String notificationId) throws TransportConfigurationException, EmailException {
        if (!emailFactory.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        final List<InternetAddress> recipientAddresses = stringsToInternetAddresses(recipients, "TO", notificationId);
        if (config.singleEmail()) {
            LOG.debug("Sending mail to {}",
                    String.join(", ", recipientAddresses.stream().map(InternetAddress::getAddress).toList()));
            final Email email = createEmailWithoutRecipients(config, model, sender, replyTo, ccEmails, bccEmails, notificationId);
            email.setTo(recipientAddresses);
            email.send();
        } else {
            for (InternetAddress recipient : recipientAddresses) {
                LOG.debug("Sending mail to {}", recipient.getAddress());
                final Email email = createEmailWithoutRecipients(config, model, sender, replyTo, ccEmails, bccEmails, notificationId);
                email.setTo(List.of(recipient));
                email.send();
            }
        }
    }

    private Email createEmailWithoutRecipients(EmailEventNotificationConfig config, Map<String, Object> model, String sender,
                                               String replyTo, Set<String> ccEmails, Set<String> bccEmails,
                                               String notificationId) throws EmailException, TransportConfigurationException {
        final Email email = createEmailWithBody(config, model);

        if (!isNullOrEmpty(sender)) {
            email.setFrom(sender);
        }

        if (email.getFromAddress() == null) {
            throw new TransportConfigurationException("No from address specified for email transport.");
        }

        if (!isNullOrEmpty(replyTo)) {
            email.addReplyTo(replyTo);
        }

        if (!ccEmails.isEmpty()) {
            email.setCc(stringsToInternetAddresses(ccEmails, "CC", notificationId));
        }

        if (!bccEmails.isEmpty()) {
            email.setBcc(stringsToInternetAddresses(bccEmails, "BCC", notificationId));
        }

        email.setSubject(buildSubject(config, model));

        return email;
    }

    private List<InternetAddress> stringsToInternetAddresses(Set<String> addresses, String line, String notificationId) {
        return addresses.stream()
                .map(address -> {
                    try {
                        LOG.debug("Converting email {} to InternetAddress.", address);
                        return new InternetAddress(address);
                    } catch (AddressException e) {
                        LOG.error("Unable to add {} to the {} line in email notification {}.",
                                address, line, notificationId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    Email createEmailWithBody(EmailEventNotificationConfig config, Map<String, Object> model) throws EmailException {
        if (!isNullOrEmpty(config.htmlBodyTemplate())) {
            HtmlEmail email = emailFactory.htmlEmail();
            email.setTextMsg(buildBody(config, model));
            email.setHtmlMsg(buildHtmlBody(config, model));
            return email;
        } else {
            SimpleEmail email = emailFactory.simpleEmail();
            email.setMsg(buildBody(config, model));
            return email;
        }
    }

    void sendEmails(Set<String> emailRecipients, Set<String> ccEmails, Set<String> bccEmails, String sender, String replyTo, EmailEventNotificationConfig config,
                    String notificationId, Map<String, Object> model) throws TransportConfigurationException, EmailException {
        if (!emailFactory.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        if (emailRecipients.isEmpty()) {
            LOG.debug("Cannot send emails: empty recipient list.");
            final Notification notification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", f("No recipients have been defined for notification with ID [%s]!", notificationId))
                    .addDetail("description", "To fix this, go to the notification configuration and add at least one alert recipient.");
            notificationService.publishIfFirst(notification);
            return;
        }

        sendEmails(config, emailRecipients, ccEmails, bccEmails, sender, replyTo, model, notificationId);
    }

}
