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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.Set;

@AutoValue
@JsonTypeName(EmailEventNotificationConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = EmailEventNotificationConfigEntity.Builder.class)
public abstract class EmailEventNotificationConfigEntity implements EventNotificationConfigEntity {

    public static final String TYPE_NAME = "email-notification-v1";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_REPLY_TO = "replyTo";
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
    private static final String FIELD_SINGLE_EMAIL = "single_email";
    private static final String FIELD_CC_USERS = "cc_users";
    private static final String FIELD_CC_EMAILS = "cc_emails";
    private static final String FIELD_LOOKUP_CC_EMAILS = "lookup_cc_emails";
    private static final String FIELD_CC_EMAILS_LOOKUP_TABLE_NAME = "cc_emails_lut_name";
    private static final String FIELD_CC_EMAILS_LOOKUP_TABLE_KEY = "cc_emails_lut_key";
    private static final String FIELD_BCC_USERS = "bcc_users";
    private static final String FIELD_BCC_EMAILS = "bcc_emails";
    private static final String FIELD_LOOKUP_BCC_EMAILS = "lookup_bcc_emails";
    private static final String FIELD_BCC_EMAILS_LOOKUP_TABLE_NAME = "bcc_emails_lut_name";
    private static final String FIELD_BCC_EMAILS_LOOKUP_TABLE_KEY = "bcc_emails_lut_key";

    @JsonProperty(FIELD_SENDER)
    public abstract ValueReference sender();

    @JsonProperty(FIELD_REPLY_TO)
    public abstract ValueReference replyTo();

    @JsonProperty(FIELD_SUBJECT)
    public abstract ValueReference subject();

    @JsonProperty(FIELD_BODY_TEMPLATE)
    public abstract ValueReference bodyTemplate();

    @JsonProperty(FIELD_HTML_BODY_TEMPLATE)
    public abstract ValueReference htmlBodyTemplate();

    @JsonProperty(FIELD_EMAIL_RECIPIENTS)
    public abstract Set<String> emailRecipients();

    @JsonProperty(FIELD_USER_RECIPIENTS)
    public abstract Set<String> userRecipients();

    @JsonProperty(FIELD_TIME_ZONE)
    public abstract ValueReference timeZone();

    @JsonProperty(FIELD_LOOKUP_RECIPIENT_EMAILS)
    public abstract ValueReference lookupRecipientEmails();

    @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_NAME)
    public abstract ValueReference recipientsLUTName();

    @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_KEY)
    public abstract ValueReference recipientsLUTKey();

    @JsonProperty(FIELD_LOOKUP_SENDER_EMAIL)
    public abstract ValueReference lookupSenderEmail();

    @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_NAME)
    public abstract ValueReference senderLUTName();

    @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_KEY)
    public abstract ValueReference senderLUTKey();

    @JsonProperty(FIELD_LOOKUP_REPLY_TO_EMAIL)
    public abstract ValueReference lookupReplyToEmail();

    @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_NAME)
    public abstract ValueReference replyToLUTName();

    @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_KEY)
    public abstract ValueReference replyToLUTKey();

    @JsonProperty(FIELD_SINGLE_EMAIL)
    public abstract ValueReference singleEmail();

    @JsonProperty(FIELD_CC_USERS)
    public abstract Set<String> ccUsers();

    @JsonProperty(FIELD_CC_EMAILS)
    public abstract Set<String> ccEmails();

    @JsonProperty(FIELD_LOOKUP_CC_EMAILS)
    public abstract ValueReference lookupCcEmails();

    @JsonProperty(FIELD_CC_EMAILS_LOOKUP_TABLE_NAME)
    public abstract ValueReference ccEmailsLUTName();

    @JsonProperty(FIELD_CC_EMAILS_LOOKUP_TABLE_KEY)
    public abstract ValueReference ccEmailsLUTKey();

    @JsonProperty(FIELD_BCC_USERS)
    public abstract Set<String> bccUsers();

    @JsonProperty(FIELD_BCC_EMAILS)
    public abstract Set<String> bccEmails();

    @JsonProperty(FIELD_LOOKUP_BCC_EMAILS)
    public abstract ValueReference lookupBccEmails();

    @JsonProperty(FIELD_BCC_EMAILS_LOOKUP_TABLE_NAME)
    public abstract ValueReference bccEmailsLUTName();

    @JsonProperty(FIELD_BCC_EMAILS_LOOKUP_TABLE_KEY)
    public abstract ValueReference bccEmailsLUTKey();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EmailEventNotificationConfigEntity.Builder()
                    .type(TYPE_NAME)
                    .htmlBodyTemplate(ValueReference.of(""))
                    .timeZone(ValueReference.of("UTC"))
                    .replyTo(ValueReference.of(""))
                    .lookupRecipientEmails(ValueReference.of(false))
                    .recipientsLUTName(ValueReference.of(""))
                    .recipientsLUTKey(ValueReference.of(""))
                    .lookupSenderEmail(ValueReference.of(false))
                    .senderLUTName(ValueReference.of(""))
                    .senderLUTKey(ValueReference.of(""))
                    .lookupReplyToEmail(ValueReference.of(false))
                    .replyToLUTName(ValueReference.of(""))
                    .replyToLUTKey(ValueReference.of(""))
                    .singleEmail(ValueReference.of(false))
                    .ccUsers(Set.of())
                    .ccEmails(Set.of())
                    .bccUsers(Set.of())
                    .bccEmails(Set.of())
                    .lookupCcEmails(ValueReference.of(false))
                    .ccEmailsLUTName(ValueReference.of(""))
                    .ccEmailsLUTKey(ValueReference.of(""))
                    .lookupBccEmails(ValueReference.of(false))
                    .bccEmailsLUTName(ValueReference.of(""))
                    .bccEmailsLUTKey(ValueReference.of(""));
        }

        @JsonProperty(FIELD_SENDER)
        public abstract Builder sender(ValueReference sender);

        @JsonProperty(FIELD_REPLY_TO)
        public abstract Builder replyTo(ValueReference sender);

        @JsonProperty(FIELD_SUBJECT)
        public abstract Builder subject(ValueReference subject);

        @JsonProperty(FIELD_BODY_TEMPLATE)
        public abstract Builder bodyTemplate(ValueReference bodyTemplate);

        @JsonProperty(FIELD_HTML_BODY_TEMPLATE)
        public abstract Builder htmlBodyTemplate(ValueReference htmlBodyTemplate);

        @JsonProperty(FIELD_EMAIL_RECIPIENTS)
        public abstract Builder emailRecipients(Set<String> emailRecipients);

        @JsonProperty(FIELD_USER_RECIPIENTS)
        public abstract Builder userRecipients(Set<String> userRecipients);

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(ValueReference timeZone);

        @JsonProperty(FIELD_LOOKUP_RECIPIENT_EMAILS)
        public abstract Builder lookupRecipientEmails(ValueReference lookupRecipientEmails);

        @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_NAME)
        public abstract Builder recipientsLUTName(ValueReference recipientsLUTName);

        @JsonProperty(FIELD_RECIPIENTS_LOOKUP_TABLE_KEY)
        public abstract Builder recipientsLUTKey(ValueReference recipientsLUTKey);

        @JsonProperty(FIELD_LOOKUP_SENDER_EMAIL)
        public abstract Builder lookupSenderEmail(ValueReference lookupSenderEmail);

        @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_NAME)
        public abstract Builder senderLUTName(ValueReference senderLUTName);

        @JsonProperty(FIELD_SENDER_LOOKUP_TABLE_KEY)
        public abstract Builder senderLUTKey(ValueReference senderLUTKey);

        @JsonProperty(FIELD_LOOKUP_REPLY_TO_EMAIL)
        public abstract Builder lookupReplyToEmail(ValueReference lookupReplyToEmail);

        @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_NAME)
        public abstract Builder replyToLUTName(ValueReference replyToLUTName);

        @JsonProperty(FIELD_REPLY_TO_LOOKUP_TABLE_KEY)
        public abstract Builder replyToLUTKey(ValueReference replyToLUTKey);

        @JsonProperty(FIELD_SINGLE_EMAIL)
        public abstract Builder singleEmail(ValueReference singleEmail);

        @JsonProperty(FIELD_CC_USERS)
        public abstract Builder ccUsers(Set<String> ccUsers);

        @JsonProperty(FIELD_CC_EMAILS)
        public abstract Builder ccEmails(Set<String> ccEmails);

        @JsonProperty(FIELD_LOOKUP_CC_EMAILS)
        public abstract Builder lookupCcEmails(ValueReference lookupCcEmails);

        @JsonProperty(FIELD_CC_EMAILS_LOOKUP_TABLE_NAME)
        public abstract Builder ccEmailsLUTName(ValueReference ccEmailsLUTName);

        @JsonProperty(FIELD_CC_EMAILS_LOOKUP_TABLE_KEY)
        public abstract Builder ccEmailsLUTKey(ValueReference ccEmailsLUTKey);

        @JsonProperty(FIELD_BCC_USERS)
        public abstract Builder bccUsers(Set<String> bccUsers);

        @JsonProperty(FIELD_BCC_EMAILS)
        public abstract Builder bccEmails(Set<String> bccEmails);

        @JsonProperty(FIELD_LOOKUP_BCC_EMAILS)
        public abstract Builder lookupBccEmails(ValueReference lookupBccEmails);

        @JsonProperty(FIELD_BCC_EMAILS_LOOKUP_TABLE_NAME)
        public abstract Builder bccEmailsLUTName(ValueReference bccEmailsLUTName);

        @JsonProperty(FIELD_BCC_EMAILS_LOOKUP_TABLE_KEY)
        public abstract Builder bccEmailsLUTKey(ValueReference bccEmailsLUTKey);

        public abstract EmailEventNotificationConfigEntity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        EmailEventNotificationConfig.Builder builder = EmailEventNotificationConfig.builder()
                .sender(sender().asString(parameters))
                .replyTo(replyTo().asString())
                .subject(subject().asString(parameters))
                .bodyTemplate(bodyTemplate().asString())
                .htmlBodyTemplate(htmlBodyTemplate().asString())
                .emailRecipients(emailRecipients())
                .userRecipients(userRecipients())
                .timeZone(DateTimeZone.forID(timeZone().asString(parameters)))
                .singleEmail(singleEmail().asBoolean(parameters))
                .ccUsers(ccUsers())
                .ccEmails(ccEmails())
                .bccUsers(bccUsers())
                .bccEmails(bccEmails());
        final boolean lookupRecipientEmails = lookupRecipientEmails().asBoolean(parameters);
        builder.lookupRecipientEmails(lookupRecipientEmails);
        if (lookupRecipientEmails) {
            builder.recipientsLUTName(recipientsLUTName().asString(parameters))
                    .recipientsLUTKey(recipientsLUTKey().asString(parameters));
        }
        final boolean lookupSenderEmail = lookupSenderEmail().asBoolean(parameters);
        builder.lookupSenderEmail(lookupSenderEmail);
        if (lookupSenderEmail) {
            builder.senderLUTName(senderLUTName().asString(parameters))
                    .senderLUTKey(senderLUTKey().asString(parameters));
        }
        final boolean lookupReplyToEmail = lookupReplyToEmail().asBoolean(parameters);
        builder.lookupReplyToEmail(lookupReplyToEmail);
        if (lookupReplyToEmail) {
            builder.replyToLUTName(replyToLUTName().asString(parameters))
                    .replyToLUTKey(replyToLUTKey().asString(parameters));
        }
        final boolean lookupCcEmails = lookupCcEmails().asBoolean(parameters);
        builder.lookupCcEmails(lookupCcEmails);
        if (lookupCcEmails) {
            builder.ccEmailsLUTName(ccEmailsLUTName().asString(parameters))
                    .ccEmailsLUTKey(ccEmailsLUTKey().asString(parameters));
        }
        final boolean lookupBccEmails = lookupBccEmails().asBoolean(parameters);
        builder.lookupBccEmails(lookupBccEmails);
        if (lookupBccEmails) {
            builder.bccEmailsLUTName(bccEmailsLUTName().asString(parameters))
                    .bccEmailsLUTKey(bccEmailsLUTKey().asString(parameters));
        }
        return builder.build();
    }
}
