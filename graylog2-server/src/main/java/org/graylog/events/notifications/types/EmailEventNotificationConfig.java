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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.contentpack.entities.EmailEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@AutoValue
@JsonTypeName(EmailEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = EmailEventNotificationConfig.Builder.class)
public abstract class EmailEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "email-notification-v1";

    private static final String DEFAULT_SENDER = "graylog@example.org";
    static final String DEFAULT_SUBJECT = "Graylog event notification: ${event_definition_title}";
    static final String DEFAULT_BODY_TEMPLATE = "--- [Event Definition] ---------------------------\n" +
            "Title:       ${event_definition_title}\n" +
            "Description: ${event_definition_description}\n" +
            "Type:        ${event_definition_type}\n" +
            "--- [Event] --------------------------------------\n" +
            "Timestamp:            ${event.timestamp}\n" +
            "Message:              ${event.message}\n" +
            "Source:               ${event.source}\n" +
            "Key:                  ${event.key}\n" +
            "Priority:             ${event.priority}\n" +
            "Alert:                ${event.alert}\n" +
            "Timestamp Processing: ${event.timestamp}\n" +
            "Timerange Start:      ${event.timerange_start}\n" +
            "Timerange End:        ${event.timerange_end}\n" +
            "Source Streams:       ${event.source_streams}\n" +
            "Fields:\n" +
            "${foreach event.fields field}  ${field.key}: ${field.value}\n" +
            "${end}\n" +
            "${if backlog}\n" +
            "--- [Backlog] ------------------------------------\n" +
            "Last messages accounting for this alert:\n" +
            "${foreach backlog message}\n" +
            "${message}\n\n" +
            "${end}\n" +
            "${end}\n" +
            "\n";

    static final String DEFAULT_HTML_BODY_TEMPLATE = "<table width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\" style=\"background-color:#f9f9f9;border:none;line-height:1.2\"><tbody>" +
            "<tr style=\"line-height:1.5\"><th colspan=\"2\" style=\"background-color:#e6e6e6\">Event Definition</th></tr>" +
            "<tr><td width=\"200px\">Title</td><td>${event_definition_title}</td></tr>" +
            "<tr><td>Description</td><td>${event_definition_description}</td></tr>" +
            "<tr><td>Type</td><td>${event_definition_type}</td></tr>" +
            "</tbody></table>" +
            "<br /><table width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\" style=\"background-color:#f9f9f9;border:none;line-height:1.2\"><tbody>" +
            "<tr><th colspan=\"2\" style=\"background-color:#e6e6e6;line-height:1.5\">Event</th></tr>" +
            "<tr><td width=\"200px\">Timestamp</td><td>${event.timestamp}</td></tr>" +
            "<tr><td>Message</td><td>${event.message}</td></tr>" +
            "<tr><td>Source</td><td>${event.source}</td></tr>" +
            "<tr><td>Key</td><td>${event.key}</td></tr>" +
            "<tr><td>Priority</td><td>${event.priority}</td></tr>" +
            "<tr><td>Alert</td><td>${event.alert}</td></tr>" +
            "<tr><td>Timestamp Processing</td><td>${event.timestamp}</td></tr>" +
            "<tr><td>Timerange Start</td><td>${event.timerange_start}</td></tr>" +
            "<tr><td>Timerange End</td><td>${event.timerange_end}</td></tr>" +
            "<tr><td>Source Streams</td><td>${event.source_streams}</td></tr>" +
            "<tr><td>Fields</td><td><ul style=\"list-style-type:square;\">${foreach event.fields field}<li>${field.key}:${field.value}</li>${end}<ul></td></tr>" +
            "</tbody></table>" +
            "${if backlog}" +
            "<br /><table width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\" style=\"background-color:#f9f9f9;border:none;line-height:1.2\"><tbody>" +
            "<tr><th style=\"background-color:#e6e6e6;line-height:1.5\">Backlog (Last messages accounting for this alert)</th></tr>" +
            "${foreach backlog message}" +
            "<tr><td>${message}</td></tr>" +
            "${end}" +
            "</tbody></table>" +
            "${end}";

    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_BODY_TEMPLATE = "body_template";
    private static final String FIELD_HTML_BODY_TEMPLATE = "html_body_template";
    private static final String FIELD_EMAIL_RECIPIENTS = "email_recipients";
    private static final String FIELD_USER_RECIPIENTS = "user_recipients";

    @JsonProperty(FIELD_SENDER)
    @NotBlank
    public abstract String sender();

    @JsonProperty(FIELD_SUBJECT)
    @NotBlank
    public abstract String subject();

    @JsonProperty(FIELD_BODY_TEMPLATE)
    public abstract String bodyTemplate();

    @JsonProperty(FIELD_HTML_BODY_TEMPLATE)
    public abstract String htmlBodyTemplate();

    @JsonProperty(FIELD_EMAIL_RECIPIENTS)
    public abstract Set<String> emailRecipients();

    @JsonProperty(FIELD_USER_RECIPIENTS)
    public abstract Set<String> userRecipients();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (sender().isEmpty()) {
            validation.addError(FIELD_SENDER, "Email Notification sender cannot be empty.");
        }
        if (subject().isEmpty()) {
            validation.addError(FIELD_SUBJECT, "Email Notification subject cannot be empty.");
        }
        if (bodyTemplate().isEmpty() && htmlBodyTemplate().isEmpty()) {
            validation.addError("body", "One of Email Notification body template or Email Notification HTML body must not be empty.");
        }
        if (emailRecipients().isEmpty() && userRecipients().isEmpty()) {
            validation.addError("recipients", "Email Notification must have email recipients or user recipients.");
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EmailEventNotificationConfig.Builder()
                    .type(TYPE_NAME)
                    .sender(DEFAULT_SENDER)
                    .subject(DEFAULT_SUBJECT)
                    .emailRecipients(ImmutableSet.of())
                    .userRecipients(ImmutableSet.of())
                    .bodyTemplate(DEFAULT_BODY_TEMPLATE)
                    .htmlBodyTemplate(DEFAULT_HTML_BODY_TEMPLATE);
        }

        @JsonProperty(FIELD_SENDER)
        public abstract Builder sender(String sender);

        @JsonProperty(FIELD_SUBJECT)
        public abstract Builder subject(String subject);

        @JsonProperty(FIELD_BODY_TEMPLATE)
        public abstract Builder bodyTemplate(String bodyTemplate);

        @JsonProperty(FIELD_HTML_BODY_TEMPLATE)
        public abstract Builder htmlBodyTemplate(String htmlBodyTemplate);

        @JsonProperty(FIELD_EMAIL_RECIPIENTS)
        public abstract Builder emailRecipients(Set<String> emailRecipients);

        @JsonProperty(FIELD_USER_RECIPIENTS)
        public abstract Builder userRecipients(Set<String> userRecipients);

        public abstract EmailEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return EmailEventNotificationConfigEntity.builder()
            .sender(ValueReference.of(sender()))
            .subject(ValueReference.of(subject()))
            .bodyTemplate(ValueReference.of(bodyTemplate()))
            .htmlBodyTemplate(ValueReference.of(htmlBodyTemplate()))
            .emailRecipients(emailRecipients())
            .userRecipients(userRecipients())
            .build();
    }
}
