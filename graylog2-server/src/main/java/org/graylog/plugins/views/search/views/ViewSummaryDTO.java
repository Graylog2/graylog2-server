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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = ViewSummaryDTO.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@WithBeanGetter
public abstract class ViewSummaryDTO {
    @ObjectId
    @Id
    @Nullable
    @JsonProperty(ViewDTO.FIELD_ID)
    public abstract String id();

    @JsonProperty(ViewDTO.FIELD_TYPE)
    public abstract ViewDTO.Type type();

    @JsonProperty(ViewDTO.FIELD_TITLE)
    @NotBlank
    public abstract String title();

    // A short, one sentence description of the view
    @JsonProperty(ViewDTO.FIELD_SUMMARY)
    public abstract String summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(ViewDTO.FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(ViewDTO.FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(ViewDTO.FIELD_PROPERTIES)
    public abstract ImmutableSet<String> properties();

    @JsonProperty(ViewDTO.FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(ViewDTO.FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(ViewDTO.FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static Set<String> idsFrom(Collection<ViewSummaryDTO> views) {
        return views.stream().map(ViewSummaryDTO::id).collect(Collectors.toSet());
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(ViewDTO.FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(ViewDTO.FIELD_TYPE)
        public abstract Builder type(ViewDTO.Type type);

        @JsonProperty(ViewDTO.FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(ViewDTO.FIELD_SUMMARY)
        public abstract Builder summary(String summary);

        @JsonProperty(ViewDTO.FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(ViewDTO.FIELD_SEARCH_ID)
        public abstract Builder searchId(String searchId);

        abstract ImmutableSet.Builder<String> propertiesBuilder();

        @JsonProperty(ViewDTO.FIELD_PROPERTIES)
        public Builder properties(Set<String> properties) {
            propertiesBuilder().addAll(properties);
            return this;
        }

        @JsonProperty(ViewDTO.FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(ViewDTO.FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(ViewDTO.FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewSummaryDTO.Builder()
                    .type(ViewDTO.Type.DASHBOARD)
                    .summary("")
                    .description("")
                    .properties(ImmutableSet.of())
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewSummaryDTO build();
    }
}
