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
package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.events.event.EventDto;
import org.graylog.events.procedures.EventProcedure;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog2.plugin.MessageSummary;

import java.util.List;
import java.util.Optional;

/**
 * Data object that can be used in notifications to provide structured data to plugins.
 */
@AutoValue
public abstract class EventNotificationModelData {
    private static final String UNKNOWN = "<unknown>";
    public static final String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
    public static final String FIELD_EVENT_DEFINITION_TYPE = "event_definition_type";
    public static final String FIELD_EVENT_DEFINITION_TITLE = "event_definition_title";
    public static final String FIELD_EVENT_DEFINITION_DESCRIPTION = "event_definition_description";
    public static final String FIELD_REMEDIATION_STEPS = "remediation_steps";
    public static final String FIELD_EVENT_PROCEDURE_TEXT = "event_procedure_text";
    public static final String FIELD_EVENT_PROCEDURE_HTML = "event_procedure_html";
    public static final String FIELD_JOB_DEFINITION_ID = "job_definition_id";
    public static final String FIELD_JOB_TRIGGER_ID = "job_trigger_id";
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_BACKLOG = "backlog";

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    public abstract String eventDefinitionId();

    @JsonProperty(FIELD_EVENT_DEFINITION_TYPE)
    public abstract String eventDefinitionType();

    @JsonProperty(FIELD_EVENT_DEFINITION_TITLE)
    public abstract String eventDefinitionTitle();

    @JsonProperty(FIELD_EVENT_DEFINITION_DESCRIPTION)
    public abstract String eventDefinitionDescription();

    @JsonProperty(FIELD_REMEDIATION_STEPS)
    public abstract String remediationSteps();

    @JsonProperty(FIELD_EVENT_PROCEDURE_TEXT)
    public abstract String eventProcedureText();

    @JsonProperty(FIELD_EVENT_PROCEDURE_HTML)
    public abstract String eventProcedureHtml();

    @JsonProperty(FIELD_JOB_DEFINITION_ID)
    public abstract String jobDefinitionId();

    @JsonProperty(FIELD_JOB_TRIGGER_ID)
    public abstract String jobTriggerId();

    @JsonProperty(FIELD_EVENT)
    public abstract EventDto event();

    @JsonProperty(FIELD_BACKLOG)
    public abstract ImmutableList<MessageSummary> backlog();

    public static Builder builder() {
        return new AutoValue_EventNotificationModelData.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder eventDefinitionId(String id);

        public abstract Builder eventDefinitionType(String type);

        public abstract Builder eventDefinitionTitle(String title);

        public abstract Builder eventDefinitionDescription(String description);

        public abstract Builder remediationSteps(String remediationSteps);

        public abstract Builder eventProcedureText(String eventProcedureText);

        public abstract Builder eventProcedureHtml(String eventProcedureHtml);

        public abstract Builder jobDefinitionId(String jobDefinitionId);

        public abstract Builder jobTriggerId(String jobTriggerId);

        public abstract Builder event(EventDto event);

        public abstract Builder backlog(List<MessageSummary> backlog);

        public abstract EventNotificationModelData build();
    }

    public static EventNotificationModelData of(EventNotificationContext ctx, List<MessageSummary> backlog) {
        return of(ctx.eventDefinition().orElse(null),
                ctx.jobTrigger().orElse(null),
                ctx.event(),
                backlog,
                ctx.eventProcedure().orElse(null));
    }

    public static EventNotificationModelData of(EventDefinitionDto definitionDto,
                                                JobTriggerDto jobTriggerDto,
                                                EventDto event,
                                                List<MessageSummary> backlog,
                                                EventProcedure eventProcedure) {
        return EventNotificationModelData.builder()
                .eventDefinitionId(definitionDto != null ? definitionDto.id() : UNKNOWN)
                .eventDefinitionType(definitionDto != null ? definitionDto.config().type() : UNKNOWN)
                .eventDefinitionTitle(definitionDto != null ? definitionDto.title() : UNKNOWN)
                .eventDefinitionDescription(definitionDto != null ? definitionDto.description() : UNKNOWN)
                .remediationSteps(Optional.ofNullable(definitionDto).map(EventDefinitionDto::remediationSteps).orElse(UNKNOWN))
                .eventProcedureText(eventProcedure != null ? eventProcedure.toText() : UNKNOWN)
                .eventProcedureHtml(eventProcedure != null ? eventProcedure.toHtml() : UNKNOWN)
                .jobDefinitionId(jobTriggerDto != null ? jobTriggerDto.jobDefinitionId() : UNKNOWN)
                .jobTriggerId(jobTriggerDto != null ? jobTriggerDto.id() : UNKNOWN)
                .event(event)
                .backlog(backlog)
                .build();
    }
}
