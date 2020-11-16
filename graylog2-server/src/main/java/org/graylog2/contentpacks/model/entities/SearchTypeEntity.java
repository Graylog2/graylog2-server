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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = SearchTypeEntity.TYPE_FIELD,
        visible = true,
        defaultImpl = SearchTypeEntity.Fallback.class)
@JsonAutoDetect
public interface SearchTypeEntity extends NativeEntityConverter<SearchType> {
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

    Builder toGenericBuilder();

    default Set<String> effectiveStreams() {
        return streams();
    }

    interface Builder {
        public abstract Builder streams(Set<String> streams);

        public abstract SearchTypeEntity build();
    }

    @JsonAutoDetect
    class Fallback implements SearchTypeEntity {

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
        public Builder toGenericBuilder() {
            return null;
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
        public SearchType toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
            return null;
        }
    }

    default Set<String> mappedStreams(Map<EntityDescriptor, Object> nativeEntities) {
        return streams().stream()
                .map(s -> EntityDescriptor.create(s, ModelTypes.STREAM_V1))
                .map(nativeEntities::get)
                .map(object -> {
                    if (object == null) {
                        throw new ContentPackException("Missing Stream for event definition");
                    } else if (object instanceof Stream) {
                        Stream stream = (Stream) object;
                        return stream.getId();
                    } else {
                        throw new ContentPackException(
                                "Invalid type for stream Stream for event definition: " + object.getClass());
                    }
                }).collect(Collectors.toSet());
    }
}
