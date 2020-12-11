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
package org.graylog2.alarmcallbacks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.graylog2.alerts.AlertSender;
import org.graylog2.alerts.EmailRecipients;
import org.graylog2.alerts.FormattedEmailAlertSender;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.ListField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EmailAlarmCallback implements AlarmCallback {
    private static final Logger LOG = LoggerFactory.getLogger(EmailAlarmCallback.class);

    public static final String CK_USER_RECEIVERS = "user_receivers";
    public static final String CK_EMAIL_RECEIVERS = "email_receivers";

    private final AlertSender alertSender;
    private final NotificationService notificationService;
    private final NodeId nodeId;
    private final EmailRecipients.Factory emailRecipientsFactory;
    private final UserService userService;
    private final EmailConfiguration emailConfiguration;
    private Configuration configuration;
    private org.graylog2.Configuration graylogConfig;

    @Inject
    public EmailAlarmCallback(AlertSender alertSender,
                              NotificationService notificationService,
                              NodeId nodeId,
                              EmailRecipients.Factory emailRecipientsFactory,
                              UserService userService,
                              EmailConfiguration emailConfiguration,
                              org.graylog2.Configuration graylogConfig) {
        this.alertSender = alertSender;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
        this.emailRecipientsFactory = emailRecipientsFactory;
        this.userService = userService;
        this.emailConfiguration = emailConfiguration;
        this.graylogConfig = graylogConfig;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        // Send alerts.
        final EmailRecipients emailRecipients = this.getEmailRecipients();
        if (emailRecipients.isEmpty()) {
            if (!emailConfiguration.isEnabled()) {
                throw new AlarmCallbackException("Email transport is not enabled in server configuration file!");
            }

            LOG.info("Alarm callback has no email recipients, not sending any emails.");
            return;
        }

        AlertCondition alertCondition = result.getTriggeredCondition();
        try {
            if (alertCondition.getBacklog() > 0 && result.getMatchingMessages() != null) {
                alertSender.sendEmails(stream, emailRecipients, result, getAlarmBacklog(result));
            } else {
                alertSender.sendEmails(stream, emailRecipients, result);
            }
        } catch (TransportConfigurationException e) {
            LOG.warn("Alarm callback has email recipients and is triggered, but email transport is not configured.");
            Notification notification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("stream_id", stream.getId())
                    .addDetail("exception", e.getMessage());
            notificationService.publishIfFirst(notification);

            throw new AlarmCallbackException(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Alarm callback has email recipients and is triggered, but sending emails failed", e);

            String exceptionDetail = e.toString();
            if (e.getCause() != null) {
                exceptionDetail += " (" + e.getCause() + ")";
            }

            Notification notification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.EMAIL_TRANSPORT_FAILED)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("stream_id", stream.getId())
                    .addDetail("exception", exceptionDetail);
            notificationService.publishIfFirst(notification);

            throw new AlarmCallbackException(e.getMessage(), e);
        }
    }

    private EmailRecipients getEmailRecipients() {
        return emailRecipientsFactory.create(
                configuration.getList(CK_USER_RECEIVERS, Collections.emptyList()),
                configuration.getList(CK_EMAIL_RECEIVERS, Collections.emptyList())
        );
    }

    protected List<Message> getAlarmBacklog(AlertCondition.CheckResult result) {
        final AlertCondition alertCondition = result.getTriggeredCondition();
        final List<MessageSummary> matchingMessages = result.getMatchingMessages();

        final int effectiveBacklogSize = Math.min(alertCondition.getBacklog(), matchingMessages.size());

        if (effectiveBacklogSize == 0) {
            return Collections.emptyList();
        }

        final List<MessageSummary> backlogSummaries = matchingMessages.subList(0, effectiveBacklogSize);

        final List<Message> backlog = Lists.newArrayListWithCapacity(effectiveBacklogSize);

        for (MessageSummary messageSummary : backlogSummaries) {
            backlog.add(messageSummary.getRawMessage());
        }

        return backlog;
    }

    @Override
    public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
        this.alertSender.initialize(configuration);
    }

    // I am truly sorry about this, but leaking the user list is not okay...
    private ConfigurationRequest getConfigurationRequest(Map<String, String> userNames) {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        if (!graylogConfig.isCloud()) {
            configurationRequest.addField(new TextField("sender",
                    "Sender",
                    "",
                    "The sender of sent out mail alerts",
                    ConfigurationField.Optional.OPTIONAL));
        }

        configurationRequest.addField(new TextField("subject",
                "E-Mail Subject",
                "Graylog alert for stream: ${stream.title}: ${check_result.resultDescription}",
                "The subject of sent out mail alerts",
                ConfigurationField.Optional.NOT_OPTIONAL));

        configurationRequest.addField(new TextField("body",
                "E-Mail Body",
                FormattedEmailAlertSender.bodyTemplate,
                "The template to generate the body from",
                ConfigurationField.Optional.OPTIONAL,
                TextField.Attribute.TEXTAREA));

        configurationRequest.addField(new ListField(CK_USER_RECEIVERS,
                "User Receivers",
                Collections.emptyList(),
                userNames,
                "Graylog usernames that should receive this alert",
                ConfigurationField.Optional.OPTIONAL));

        configurationRequest.addField(new ListField(CK_EMAIL_RECEIVERS,
                "E-Mail Receivers",
                Collections.emptyList(),
                Collections.emptyMap(),
                "E-Mail addresses that should receive this alert",
                ConfigurationField.Optional.OPTIONAL,
                ListField.Attribute.ALLOW_CREATE));

        return configurationRequest;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return getConfigurationRequest(Collections.emptyMap());
    }

    /* This method should be used when we want to provide user auto-completion to users that have permissions for it */
    public ConfigurationRequest getEnrichedRequestedConfiguration() {
        final Map<String, String> regularUsers = userService.loadAll().stream()
                .collect(Collectors.toMap(User::getName, User::getName));

        final Map<String, String> userNames = ImmutableMap.<String, String>builder()
                .put(graylogConfig.getRootUsername(), graylogConfig.getRootUsername())
                .putAll(regularUsers)
                .build();

        return getConfigurationRequest(userNames);
    }

    @Override
    public String getName() {
        return "Email Alarm Callback [Deprecated]";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        final boolean missingSender = isNullOrEmpty(configuration.getString("sender")) && isNullOrEmpty(emailConfiguration.getFromEmail());
        if (missingSender || isNullOrEmpty(configuration.getString("subject"))) {
            throw new ConfigurationException("Sender or subject are missing or invalid.");
        }
    }
}
