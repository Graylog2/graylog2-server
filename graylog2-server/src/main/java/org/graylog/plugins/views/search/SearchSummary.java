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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.graylog.plugins.views.search.Search.FIELD_CREATED_AT;
import static org.graylog.plugins.views.search.Search.FIELD_OWNER;
import static org.graylog.plugins.views.search.Search.FIELD_REQUIRES;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SearchSummary.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SearchSummary {
    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder {
        @Id
        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        abstract SearchSummary autoBuild();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SearchSummary.Builder()
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public SearchSummary build() {
            final SearchSummary search = autoBuild();
            return search;
        }
    }
}
