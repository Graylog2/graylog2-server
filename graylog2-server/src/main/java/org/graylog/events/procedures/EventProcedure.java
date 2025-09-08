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
package org.graylog.events.procedures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog.events.event.EventDto;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.security.html.HTMLSanitizerConverter;
import org.mongojack.Id;

import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

@AutoValue
@JsonDeserialize(builder = EventProcedure.Builder.class)
public abstract class EventProcedure implements ScopedEntity<EventProcedure.Builder> {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STEPS = "steps";

    @Nullable
    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @Nullable
    @JsonProperty(FIELD_DESCRIPTION)
    @JsonSerialize(converter = HTMLSanitizerConverter.class)
    public abstract String description();

    @JsonProperty(FIELD_STEPS)
    public abstract List<EventProcedureStep> steps();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements ScopedEntity.Builder<Builder> {

        @Override
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @Override
        @JsonProperty(FIELD_SCOPE)
        public abstract Builder scope(String scope);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_STEPS)
        public abstract Builder steps(List<EventProcedureStep> steps);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventProcedure.Builder().scope(DefaultEntityScope.NAME);
        }

        public abstract EventProcedure build();
    }

    public String toText(EventDto event) {
        final StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(f("""
                --- [Event Procedure] ----------------------------
                Title:       %s
                Description: %s""", title(), description()));

        if (steps() != null && !steps().isEmpty()) {
            textBuilder.append("\n\n").append("Steps:");
            for (int i = 0; i < steps().size(); i++) {
                final EventProcedureStep step = steps().get(i);
                textBuilder.append("\n")
                        .append(f("%d. %s\n\t%s\n\t%s", i + 1, step.title(), step.description(), step.toText(event)));
            }
        }

        return textBuilder.toString();
    }

    public String toHtml(EventDto event) {
        final StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("""
                <table width="100%" border="0" cellpadding="10" cellspacing="0" style="background-color:#f9f9f9;border:none;line-height:1.2"><tbody>
                <tr style="line-height:1.5"><th colspan="3" style="background-color:#e6e6e6">Event Procedure</th></tr>
                """);
        htmlBuilder.append(f("""
                <tr><td width="200px">Title</td><td>%s</td></tr>
                """, title()));
        htmlBuilder.append(f("""
                <tr><td>Description</td><td>%s</td></tr>
                """, description()));

        if (steps() != null && !steps().isEmpty()) {
            htmlBuilder.append("""
                    <tr><td><Strong>Steps</Strong></td>
                    """);
            for (int i = 0; i < steps().size(); i++) {
                final EventProcedureStep step = steps().get(i);
                htmlBuilder.append(f("""
                        <tr><td>%d. %s</td><td>%s</td>
                        """, i + 1, step.title(), step.description()));
                htmlBuilder.append(step.toHtml(event));
            }
        }

        htmlBuilder.append("""
                </tbody></table>
                """);

        return htmlBuilder.toString();
    }
}
