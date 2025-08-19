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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Redirects the frontend to an existing dashboard.
 */
public class GoToDashboard extends Action {
    public static final String NAME = "go_to_dashboard";
    public static final String FIELD_DASHBOARD_ID = "dashboard_id";
    public static final String FIELD_PARAMETERS = "parameters";

    @Inject
    public GoToDashboard(@Assisted ActionDto dto) {
        super(dto);
    }

    public interface Factory extends Action.Factory<GoToDashboard> {
        @Override
        GoToDashboard create(ActionDto dto);
    }

    @AutoValue
    @JsonAutoDetect
    @JsonTypeName(NAME)
    @JsonDeserialize(builder = AutoValue_GoToDashboard_Config.Builder.class)
    public static abstract class Config implements ActionConfig {
        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty(FIELD_DASHBOARD_ID)
        public abstract String dashboardId();

        @Nullable
        @JsonProperty(FIELD_PARAMETERS)
        public abstract Map<String, String> parameters();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @JsonIgnore
        @Override
        public String toHtml() {
            return f("""
                    <td><a href="%s" target="_blank">Go to Dashboard</a></td>
                    """, getLink());
        }

        @JsonIgnore
        private String getLink() {
            final StringBuilder link = new StringBuilder("${http_external_uri}");
            link.append("dashboards/").append(dashboardId());
            if (parameters() != null && !parameters().isEmpty()) {
                link.append("?");
                link.append(String.join("&", parameters().entrySet().stream()
                        .map(p -> p.getKey() + "=" + p.getValue())
                        .toList()));
            }

            return link.toString();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_DASHBOARD_ID)
            public abstract Builder dashboardId(String dashboardId);

            @JsonProperty(FIELD_PARAMETERS)
            public abstract Builder parameters(Map<String, String> parameters);

            @JsonCreator
            public static Builder create() {
                return new AutoValue_GoToDashboard_Config.Builder()
                        .type(NAME)
                        .parameters(Map.of());
            }

            public Config build() {
                if (parameters() == null) {
                    parameters(Collections.emptyMap());
                }
                return autoBuild();
            }

            abstract Config autoBuild();

            abstract Map<String, String> parameters();
        }
    }
}
