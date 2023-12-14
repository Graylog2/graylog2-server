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
import com.google.common.base.Strings;
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
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
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
            "Alert Replay:         ${http_external_uri}alerts/${event.id}/replay-search\n" +
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

    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_REPLY_TO = "reply_to";
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_BODY_TEMPLATE = "body_template";
    private static final String FIELD_HTML_BODY_TEMPLATE = "html_body_template";
    private static final String FIELD_EMAIL_RECIPIENTS = "email_recipients";
    private static final String FIELD_USER_RECIPIENTS = "user_recipients";
    private static final String FIELD_TIME_ZONE = "time_zone";
    private static final String FIELD_LOOKUP_RECIPIENT_EMAILS = "lookup_recipient_emails";
    private static final String FIELD_RECIPIENTS_LOOKUP_TABLE_NAME = "recipients_lut_name";
    private static final String FIELD_RECIPIENTS_LOOKUP_TABLE_KEY = "recipients_lut_key";
    private static final String FIELD_LOOKUP_SENDER_EMAIL = "lookup_sender_email";
    private static final String FIELD_SENDER_LOOKUP_TABLE_NAME = "sender_lut_name";
    private static final String FIELD_SENDER_LOOKUP_TABLE_KEY = "sender_lut_key";
    private static final String FIELD_LOOKUP_REPLY_TO_EMAIL = "lookup_reply_to_email";
    private static final String FIELD_REPLY_TO_LOOKUP_TABLE_NAME = "reply_to_lut_name";
    private static final String FIELD_REPLY_TO_LOOKUP_TABLE_KEY = "reply_to_lut_key";

    @JsonProperty(FIELD_SENDER)
    public abstract String sender();

    @JsonProperty(FIELD_REPLY_TO)
    public abstract String replyTo();

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

    @JsonProperty(FIELD_TIME_ZONE)
    public abstract DateTimeZone timeZone();

    @JsonProperty(FIELD_LOOKUP_RECIPIENT_EMAILS)
    public abstract boolean lookupRecipientEmails();

    @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_NAME)
    @Nullable
    public abstract String recipientsLUTName();

    @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_KEY)
    @Nullable
    public abstract String recipientsLUTKey();

    @JsonProperty(FIELD_LOOKUP_SENDER_EMAIL)
    public abstract boolean lookupSenderEmail();

    @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_NAME)
    @Nullable
    public abstract String senderLUTName();

    @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_KEY)
    @Nullable
    public abstract String senderLUTKey();

    @JsonProperty(FIELD_LOOKUP_REPLY_TO_EMAIL)
    public abstract boolean lookupReplyToEmail();

    @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_NAME)
    @Nullable
    public abstract String replyToLUTName();

    @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_KEY)
    @Nullable
    public abstract String replyToLUTKey();

    @Override
    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (subject().isEmpty()) {
            validation.addError(FIELD_SUBJECT, "Email Notification subject cannot be empty.");
        }
        if (bodyTemplate().isEmpty() && htmlBodyTemplate().isEmpty()) {
            validation.addError("body", "One of Email Notification body template or Email Notification HTML body must not be empty.");
        }
        if (!lookupRecipientEmails() && emailRecipients().isEmpty() && userRecipients().isEmpty()) {
            validation.addError("recipients", "Email Notification must have email recipients or user recipients.");
        }
        if (lookupRecipientEmails()) {
            if (Strings.isNullOrEmpty(recipientsLUTName())) {
                validation.addError(FIELD_RECIPIENTS_LOOKUP_TABLE_NAME, "Lookup table name must not be empty");
            }
            if (Strings.isNullOrEmpty(recipientsLUTKey())) {
                validation.addError(FIELD_RECIPIENTS_LOOKUP_TABLE_KEY, "Lookup table key must not be empty");
            }
        }
        if (lookupSenderEmail()) {
            if (Strings.isNullOrEmpty(senderLUTName())) {
                validation.addError(FIELD_SENDER_LOOKUP_TABLE_NAME, "Lookup table name must not be empty");
            }
            if (Strings.isNullOrEmpty(senderLUTKey())) {
                validation.addError(FIELD_SENDER_LOOKUP_TABLE_KEY, "Lookup table key must not be empty");
            }
        }
        if (lookupReplyToEmail()) {
            if (Strings.isNullOrEmpty(replyToLUTName())) {
                validation.addError(FIELD_REPLY_TO_LOOKUP_TABLE_NAME, "Lookup table name must not be empty");
            }
            if (Strings.isNullOrEmpty(replyToLUTKey())) {
                validation.addError(FIELD_REPLY_TO_LOOKUP_TABLE_KEY, "Lookup table key must not be empty");
            }
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
                    .replyTo("")
                    .subject(DEFAULT_SUBJECT)
                    .emailRecipients(ImmutableSet.of())
                    .userRecipients(ImmutableSet.of())
                    .bodyTemplate(DEFAULT_BODY_TEMPLATE)
                    .timeZone(DateTimeZone.UTC)
                    .htmlBodyTemplate("")
                    .lookupRecipientEmails(false)
                    .lookupSenderEmail(false)
                    .lookupReplyToEmail(false);
        }

        @JsonProperty(FIELD_SENDER)
        public abstract Builder sender(String sender);

        @JsonProperty(FIELD_REPLY_TO)
        public abstract Builder replyTo(String sender);

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

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(DateTimeZone timeZone);

        @JsonProperty(FIELD_LOOKUP_RECIPIENT_EMAILS)
        public abstract Builder lookupRecipientEmails(boolean lookupRecipientEmails);

        @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_NAME)
        public abstract Builder recipientsLUTName(String recipientsLUTName);

        @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_KEY)
        public abstract Builder recipientsLUTKey(String recipientsLUTKey);

        @JsonProperty(FIELD_LOOKUP_SENDER_EMAIL)
        public abstract Builder lookupSenderEmail(boolean lookupSenderEmail);

        @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_NAME)
        public abstract Builder senderLUTName(String senderLUTName);

        @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_KEY)
        public abstract Builder senderLUTKey(String senderLUTKey);

        @JsonProperty(FIELD_LOOKUP_REPLY_TO_EMAIL)
        public abstract Builder lookupReplyToEmail(boolean lookupReplyToEmail);

        @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_NAME)
        public abstract Builder replyToLUTName(String replyToLUTName);

        @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_KEY)
        public abstract Builder replyToLUTKey(String replyToLUTKey);

        public abstract EmailEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        EmailEventNotificationConfigEntity.Builder builder = EmailEventNotificationConfigEntity.builder()
                .sender(ValueReference.of(sender()))
                .replyTo(ValueReference.of(replyTo()))
                .subject(ValueReference.of(subject()))
                .bodyTemplate(ValueReference.of(bodyTemplate()))
                .htmlBodyTemplate(ValueReference.of(htmlBodyTemplate()))
                .emailRecipients(emailRecipients())
                .userRecipients(userRecipients())
                .timeZone(ValueReference.of(timeZone().getID()))
                .lookupRecipientEmails(ValueReference.of(lookupRecipientEmails()))
                .lookupSenderEmail(ValueReference.of(lookupSenderEmail()))
                .lookupReplyToEmail(ValueReference.of(lookupReplyToEmail()));
        if (lookupRecipientEmails()) {
            builder.recipientsLUTName(ValueReference.ofNullable(recipientsLUTName()))
                    .recipientsLUTKey(ValueReference.ofNullable(recipientsLUTKey()));
        } else {
            builder.recipientsLUTName(ValueReference.of(""))
                    .recipientsLUTKey(ValueReference.of(""));
        }
        if (lookupSenderEmail()) {
            builder.senderLUTName(ValueReference.ofNullable(senderLUTName()))
                    .senderLUTKey(ValueReference.ofNullable(senderLUTKey()));
        } else {
            builder.senderLUTName(ValueReference.of(""))
                    .senderLUTKey(ValueReference.of(""));
        }
        if (lookupReplyToEmail()) {
            builder.replyToLUTName(ValueReference.ofNullable(replyToLUTName()))
                    .replyToLUTKey(ValueReference.ofNullable(replyToLUTKey()));
        } else {
            builder.replyToLUTName(ValueReference.of(""))
                    .replyToLUTKey(ValueReference.of(""));
        }

        return builder.build();
    }
}
