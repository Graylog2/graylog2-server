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
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.security.html.HTMLSanitizerConverter;

import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

@AutoValue
@JsonDeserialize(builder = EventProcedure.Builder.class)
public abstract class EventProcedure extends ScopedEntity {
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
    public static abstract class Builder extends ScopedEntity.AbstractBuilder<Builder> {

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_STEPS)
        public abstract Builder steps(List<EventProcedureStep> steps);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventProcedure.Builder();
        }

        public abstract EventProcedure build();
    }

    public String toTemplate() {
        final StringBuilder procedureBuilder = new StringBuilder();
        procedureBuilder.append("--- [Event Procedures ---------------------------\n");
        procedureBuilder.append("Title:       " + title() + "\n");
        procedureBuilder.append("Description: " + description() + "\n");
        procedureBuilder.append("Steps:\n");

        return procedureBuilder.toString();
    }

    public String toHtml() {
        final StringBuilder procedureBuilder = new StringBuilder();
        procedureBuilder.append(f("""
                <section>
                  <h1>Event Procedures</h1>
                  <header>
                    <h2>%s</h2>
                  </header>""", title()));

        procedureBuilder.append(f("""
                <section>
                  <h3>Description</h3>
                  <p>No description set</p>
                </section>""", title()));

        if (steps() != null && !steps().isEmpty()) {
            procedureBuilder.append("""
                    <section>
                      <h3>Event Procedure Steps</h3>
                      <ol>""");
            steps().forEach(step -> {procedureBuilder.append(step.toHtml());});
            procedureBuilder.append("""
                      </ol>
                    </section>""");
        }

        procedureBuilder.append("""
                </section>""");
        return procedureBuilder.toString();
    }
}
