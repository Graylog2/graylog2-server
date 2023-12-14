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
import com.google.common.collect.ImmutableList;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.alerts.EmailRecipients;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

public class EmailEventNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory {
        @Override
        EmailEventNotification create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(EmailEventNotification.class);

    private final EventNotificationService notificationCallbackService;
    private final EmailSender emailSender;
    private final NotificationService notificationService;
    private final NodeId nodeId;
    private final LookupTableService lookupTableService;
    private final ObjectMapperProvider objectMapperProvider;
    private final URI httpExternalUri;
    private final EmailRecipients.Factory emailRecipientsFactory;
    private final Engine templateEngine;

    @Inject
    public EmailEventNotification(EventNotificationService notificationCallbackService,
                                  EmailSender emailSender,
                                  NotificationService notificationService,
                                  NodeId nodeId,
                                  LookupTableService lookupTableService,
                                  ObjectMapperProvider objectMapperProvider,
                                  HttpConfiguration httpConfiguration,
                                  EmailRecipients.Factory emailRecipientsFactory,
                                  Engine templateEngine) {
        this.notificationCallbackService = notificationCallbackService;
        this.emailSender = emailSender;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
        this.lookupTableService = lookupTableService;
        this.objectMapperProvider = objectMapperProvider;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.emailRecipientsFactory = emailRecipientsFactory;
        this.templateEngine = templateEngine;
    }

    @Override
    public void execute(EventNotificationContext ctx) throws TemporaryEventNotificationException, PermanentEventNotificationException {
        final EmailEventNotificationConfig config = (EmailEventNotificationConfig) ctx.notificationConfig();

        try {
            ImmutableList<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
            final Map<String, Object> model = getModel(ctx, backlog, config.timeZone());
            final EmailRecipients emailRecipients = emailRecipientsFactory.create(
                    new ArrayList<>(config.userRecipients()),
                    getEmails(config, model)
            );
            final String sender = getSender(config, model);
            final String replyTo = getReplyTo(config, model);
            emailSender.sendEmails(emailRecipients, sender, replyTo, config, ctx.notificationId(), model);
        } catch (ConfigurationError e) {
            throw new TemporaryEventNotificationException(e.getMessage());
        } catch (TransportConfigurationException e) {
            Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.EMAIL_TRANSPORT_CONFIGURATION_INVALID)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("exception", e.getMessage());
            notificationService.publishIfFirst(systemNotification);

            throw new TemporaryEventNotificationException("Notification has email recipients and is triggered, but email transport is not configured. " +
                    e.getMessage());
        } catch (Exception e) {
            String exceptionDetail = e.toString();
            if (e.getCause() != null) {
                exceptionDetail += " (" + e.getCause() + ")";
            }

            final Notification systemNotification = notificationService.buildNow()
                    .addNode(nodeId.getNodeId())
                    .addType(Notification.Type.EMAIL_TRANSPORT_FAILED)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("exception", exceptionDetail);
            notificationService.publishIfFirst(systemNotification);

            throw new PermanentEventNotificationException("Notification has email recipients and is triggered, but sending emails failed. " +
                    e.getMessage());
        }

        LOG.debug("Sending email to addresses <{}> and users <{}> using notification <{}>",
                String.join(",", config.emailRecipients()),
                String.join(",", config.userRecipients()),
                ctx.notificationId());
    }

    private Map<String, Object> getModel(EventNotificationContext ctx, ImmutableList<MessageSummary> backlog, DateTimeZone timeZone) {
        final EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);
        Map<String, Object> model = objectMapperProvider.getForTimeZone(timeZone).convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        model.put("http_external_uri", this.httpExternalUri);
        return model;
    }

    private String getSender(EmailEventNotificationConfig config, Map<String, Object> model) throws ConfigurationError {
        String sender = config.sender();
        if (config.lookupSenderEmail()) {
            LookupResult result = getLookupResult(config.senderLUTName(), config.senderLUTKey(), model);
            if (result != null) {
                if (lookupResultHasValue(result, config.senderLUTName(), config.senderLUTKey())) {
                    sender = result.singleValue() == null ? null : result.singleValue().toString();
                }
            }
        }
        return sender;
    }

    private String getReplyTo(EmailEventNotificationConfig config, Map<String, Object> model) throws ConfigurationError {
        String replyTo = config.replyTo();
        if (config.lookupReplyToEmail()) {
            LookupResult result = getLookupResult(config.replyToLUTName(), config.replyToLUTKey(), model);
            if (result != null) {
                if (lookupResultHasValue(result, config.replyToLUTName(), config.replyToLUTKey())) {
                    replyTo = result.singleValue() == null ? null : result.singleValue().toString();
                }
            }
        }
        return replyTo;
    }

    private List<String> getEmails(EmailEventNotificationConfig config, Map<String, Object> model) throws ConfigurationError {
        List<String> emails = new ArrayList<>(config.emailRecipients());
        if (config.lookupRecipientEmails()) {
            LookupResult result = getLookupResult(config.recipientsLUTName(), config.recipientsLUTKey(), model);
            if (result != null) {
                if (lookupResultHasValue(result, config.recipientsLUTName(), config.recipientsLUTKey())) {
                    if (result.stringListValue() != null && !result.stringListValue().isEmpty()) {
                        emails = result.stringListValue();
                    } else if (result.multiValue() != null && !result.multiValue().isEmpty()) {
                        emails = result.multiValue().values().stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                    } else {
                        emails = List.of(requireNonNull(result.singleValue()).toString());
                    }
                }
            }
        }
        return emails;
    }

    private LookupResult getLookupResult(String tableName, String keyTemplate, Map<String, Object> model) throws ConfigurationError {
        LookupTable lut = lookupTableService.getTable(tableName);
        if (lut == null) {
            throw new ConfigurationError(f("Unable to find lookup table with name [%s] to get recipient emails.", tableName));
        }
        String key = templateEngine.transform(keyTemplate, model);
        return lut.lookup(requireNonNull(key));
    }

    private boolean lookupResultHasValue(LookupResult result, String tableName, String key) {
        boolean lookupResultHasValue = true;
        if (result.hasError()) {
            LOG.warn("Lookup result in table [{}] with key [{}] returned with error. No recipients will be added from lookup table.",
                    tableName, key);
            lookupResultHasValue = false;
        } else {
            if (result.isEmpty()) {
                LOG.warn("Lookup result in table [{}] with key [{}] returned empty result. No recipients will be added from lookup table.",
                        tableName, key);
                lookupResultHasValue = false;
            }
        }
        return lookupResultHasValue;
    }

    static class ConfigurationError extends Exception {
        ConfigurationError(String message) {
            super(message);
        }
    }
}
