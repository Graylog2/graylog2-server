/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.events.notifications.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;

import javax.validation.constraints.NotBlank;
import java.util.Set;

@AutoValue
@JsonTypeName(EmailEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = EmailEventNotificationConfig.Builder.class)
public abstract class EmailEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "email-notification-v1";

    public static final String DEFAULT_SENDER = "graylog@example.org";
    public static final String DEFAULT_SUBJECT = "Graylog event notification: ${event_definition_title}";
    public static final String DEFAULT_BODY_TEMPLATE = "--- [Event Definition] ---------------------------\n" +
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
    private static final String FIELD_SUBJECT = "subject";
    private static final String FIELD_BODY_TEMPLATE = "body_template";
    private static final String FIELD_EMAIL_RECIPIENTS = "email_recipients";
    private static final String FIELD_USER_RECIPIENTS = "user_recipients";

    @JsonProperty(FIELD_SENDER)
    @NotBlank
    public abstract String sender();

    @JsonProperty(FIELD_SUBJECT)
    @NotBlank
    public abstract String subject();

    @JsonProperty(FIELD_BODY_TEMPLATE)
    @NotBlank
    public abstract String bodyTemplate();

    @JsonProperty(FIELD_EMAIL_RECIPIENTS)
    public abstract Set<String> emailRecipients();

    @JsonProperty(FIELD_USER_RECIPIENTS)
    public abstract Set<String> userRecipients();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
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
                    .bodyTemplate(DEFAULT_BODY_TEMPLATE);
        }

        @JsonProperty(FIELD_SENDER)
        public abstract Builder sender(String sender);

        @JsonProperty(FIELD_SUBJECT)
        public abstract Builder subject(String subject);

        @JsonProperty(FIELD_BODY_TEMPLATE)
        public abstract Builder bodyTemplate(String bodyTemplate);

        @JsonProperty(FIELD_EMAIL_RECIPIENTS)
        public abstract Builder emailRecipients(Set<String> emailRecipients);

        @JsonProperty(FIELD_USER_RECIPIENTS)
        public abstract Builder userRecipients(Set<String> userRecipients);

        public abstract EmailEventNotificationConfig build();
    }
}
