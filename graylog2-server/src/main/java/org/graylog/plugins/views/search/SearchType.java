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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.SearchTypeEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A search type represents parts of a query that generates a {@see Result result}.
 * <p>
 * Plain queries only select a set of data but by themselves do not return any specific parts from it.
 * Typical search types are aggregations across fields, a list of messages and other metadata.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = SearchType.TYPE_FIELD,
        visible = true,
        defaultImpl = SearchType.Fallback.class)
@JsonAutoDetect
public interface SearchType extends ContentPackable<SearchTypeEntity> {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    @JsonProperty("id")
    String id();

    @JsonProperty
    Optional<String> name();

    @Nullable
    @JsonProperty("filter")
    Filter filter();

    @JsonProperty
    Optional<DerivedTimeRange> timerange();

    @JsonProperty
    Optional<BackendQuery> query();

    @JsonProperty
    Set<String> streams();

    SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state);

    default Set<String> effectiveStreams() {
        return streams();
    }

    /**
     * Each search type should declare an implementation of its result conforming to this interface.
     * <p>
     * The frontend components then make use of the structured data to display it.
     */
    interface Result {
        @JsonProperty("id")
        String id();

        /**
         * The json type info property of the surrounding SearchType class. Must be set manually by subclasses.
         */
        @JsonProperty("type")
        String type();

        @JsonProperty
        Optional<String> name();
    }

    @JsonAutoDetect
    class Fallback implements SearchType {

        @JsonProperty
        private String type;

        @JsonProperty
        private String id;

        @JsonProperty
        private Optional<String> name;

        private Map<String, Object> props = Maps.newHashMap();

        @Nullable
        @JsonProperty
        private Filter filter;

        @Nullable
        @JsonProperty
        private DerivedTimeRange timeRange;

        @Nullable
        @JsonProperty
        private BackendQuery query;

        @JsonProperty
        private Set<String> streams;

        @Override
        public String type() {
            return type;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Optional<String> name() {
            return name;
        }

        @Override
        public Filter filter() {
            return filter;
        }

        @Override
        public Optional<DerivedTimeRange> timerange() {
            return Optional.ofNullable(this.timeRange);
        }

        @Override
        public Optional<BackendQuery> query() {
            return Optional.ofNullable(this.query);
        }

        @Override
        public Set<String> streams() {
            return this.streams == null ? Collections.emptySet() : this.streams;
        }

        @Override
        public SearchType applyExecutionContext(ObjectMapper objectMapper, JsonNode state) {
            return this;
        }

        @JsonAnySetter
        public void setProperties(String key, Object value) {
            props.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return props;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Fallback fallback = (Fallback) o;
            return Objects.equals(type, fallback.type) &&
                    Objects.equals(id, fallback.id) &&
                    Objects.equals(props, fallback.props);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, props);
        }

        @Override
        public SearchTypeEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
            return null;
        }
    }

    default Set<String> mappedStreams(EntityDescriptorIds entityDescriptorIds) {
        return streams().stream()
                .map(streamId -> entityDescriptorIds.get(EntityDescriptor.create(streamId, ModelTypes.STREAM_V1)))
                .map(optionalStreamId -> optionalStreamId.orElseThrow(() ->
                        new ContentPackException("Did not find matching stream id")))
                .collect(Collectors.toSet());
    }
}
