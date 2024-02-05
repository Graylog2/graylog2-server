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
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.alerts.EmailRecipients;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.email.EmailFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Map;
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

    private void sendEmail(EmailEventNotificationConfig config, String emailAddress, String sender, String replyTo, Map<String, Object> model) throws TransportConfigurationException, EmailException {
        LOG.debug("Sending mail to " + emailAddress);
        if (!emailFactory.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        final Email email = createEmailWithBody(config, model);

        if (!isNullOrEmpty(sender)) {
            email.setFrom(sender);
        }

        if (!isNullOrEmpty(replyTo)) {
            email.addReplyTo(replyTo);
        }

        if (email.getFromAddress() == null) {
            throw new TransportConfigurationException("No from address specified for email transport.");
        }

        email.setSubject(buildSubject(config, model));
        email.addTo(emailAddress);

        email.send();
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

    // TODO: move EmailRecipients class to events code
    void sendEmails(EmailRecipients emailRecipients, String sender, String replyTo, EmailEventNotificationConfig config,
                    String notificationId, Map<String, Object> model) throws TransportConfigurationException, EmailException {
        if (!emailFactory.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        if (emailRecipients.isEmpty()) {
            LOG.debug("Cannot send emails: empty recipient list.");
            return;
        }

        final Set<String> recipientsSet = emailRecipients.getEmailRecipients();
        if (recipientsSet.isEmpty()) {
            final Notification notification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", f("No recipients have been defined for notification with ID [%s]!", notificationId))
                    .addDetail("description", "To fix this, go to the notification configuration and add at least one alert recipient.");
            notificationService.publishIfFirst(notification);
        }


        for (String email : recipientsSet) {
            sendEmail(config, email, sender, replyTo, model);
        }
    }

}
