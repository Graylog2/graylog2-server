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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = ViewDTO.Builder.class)
@WithBeanGetter
public abstract class ViewDTO implements ContentPackable<ViewEntity.Builder> {
    public enum Type {
        SEARCH,
        DASHBOARD
    }

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SEARCH_ID = "search_id";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_REQUIRES = "requires";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

    public static final ImmutableSet<String> SORT_FIELDS = ImmutableSet.of(FIELD_ID, FIELD_TITLE, FIELD_CREATED_AT);

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract Type type();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    // A short, one sentence description of the view
    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(FIELD_PROPERTIES)
    public abstract ImmutableSet<String> properties();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_STATE)
    public abstract Map<String, ViewStateDTO> state();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static Set<String> idsFrom(Collection<ViewDTO> views) {
        return views.stream().map(ViewDTO::id).collect(Collectors.toSet());
    }

    public Optional<ViewStateDTO> findQueryContainingWidgetId(String widgetId) {
        return state()
                .values()
                .stream()
                .filter(viewStateDTO -> viewStateDTO.widgets()
                        .stream()
                        .map(WidgetDTO::id)
                        .collect(Collectors.toSet())
                        .contains(widgetId))
                .findFirst();
    }

    public Optional<WidgetDTO> findWidgetById(String widgetId) {
        return state().values()
                .stream()
                .flatMap(q -> q.widgets().stream())
                .filter(w -> w.id().equals(widgetId))
                .findFirst();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(Type type);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_SUMMARY)
        public abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_SEARCH_ID)
        public abstract Builder searchId(String searchId);

        abstract ImmutableSet.Builder<String> propertiesBuilder();

        @JsonProperty(FIELD_PROPERTIES)
        public Builder properties(Set<String> properties) {
            propertiesBuilder().addAll(properties);
            return this;
        }

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_STATE)
        public abstract Builder state(Map<String, ViewStateDTO> state);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewDTO.Builder()
                    .type(Type.DASHBOARD)
                    .summary("")
                    .description("")
                    .properties(ImmutableSet.of())
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewDTO build();
    }

    @Override
    public ViewEntity.Builder toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        final Map<String, ViewStateEntity> viewStateMap = new LinkedHashMap<>(this.state().size());
        for (Map.Entry<String, ViewStateDTO> entry : this.state().entrySet()) {
            final ViewStateDTO viewStateDTO = entry.getValue();
            final ViewStateEntity viewStateEntity = viewStateDTO.toContentPackEntity(entityDescriptorIds);
            viewStateMap.put(entry.getKey(), viewStateEntity);
        }

        final ViewEntity.Builder viewEntityBuilder = ViewEntity.builder()
                .type(this.type())
                .title(ValueReference.of(this.title()))
                .summary(ValueReference.of(this.summary()))
                .description(ValueReference.of(this.description()))
                .state(viewStateMap)
                .requires(this.requires())
                .properties(this.properties())
                .createdAt(this.createdAt());

        if (this.owner().isPresent()) {
            viewEntityBuilder.owner(this.owner().get());
        }

        return viewEntityBuilder;
    }
}
