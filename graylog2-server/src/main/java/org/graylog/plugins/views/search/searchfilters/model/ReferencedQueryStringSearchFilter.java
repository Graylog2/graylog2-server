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
package org.graylog.plugins.views.search.searchfilters.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonTypeName(UsedSearchFilter.REFERENCED_SEARCH_FILTER)
@JsonDeserialize(builder = ReferencedQueryStringSearchFilter.Builder.class)
public abstract class ReferencedQueryStringSearchFilter implements ReferencedSearchFilter, QueryStringSearchFilter {

    @JsonProperty(ID_FIELD)
    @Override
    public abstract String id();

    @JsonProperty(TITLE_FIELD)
    @Nullable
    public abstract String title();

    @JsonProperty(DESCRIPTION_FIELD)
    @Nullable
    public abstract String description();

    @JsonProperty(QUERY_STRING_FIELD)
    @Nullable
    public abstract String queryString();

    @Override
    @JsonProperty(value = NEGATION_FIELD, defaultValue = "false")
    public abstract boolean negation();

    @Override
    @JsonProperty(value = DISABLED_FIELD, defaultValue = "false")
    public abstract boolean disabled();

    public static ReferencedQueryStringSearchFilter create(final String id) {
        return builder().id(id).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @Override
    public UsedSearchFilter withQueryString(String queryString) {
        return toBuilder().queryString(queryString).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty
        public abstract Builder id(String id);

        @JsonProperty(TITLE_FIELD)
        public abstract Builder title(String title);

        @JsonProperty(DESCRIPTION_FIELD)
        public abstract Builder description(String description);

        @JsonProperty(QUERY_STRING_FIELD)
        public abstract Builder queryString(String queryString);

        @JsonProperty(value = NEGATION_FIELD, defaultValue = "false")
        public abstract Builder negation(boolean negation);

        @JsonProperty(value = DISABLED_FIELD, defaultValue = "false")
        public abstract Builder disabled(boolean disabled);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ReferencedQueryStringSearchFilter.Builder()
                    .disabled(false)
                    .negation(false);
        }

        public abstract ReferencedQueryStringSearchFilter build();
    }

    @Override
    public InlineQueryStringSearchFilter toInlineRepresentation() {
        return InlineQueryStringSearchFilter.builder()
                .queryString(this.queryString())
                .description(this.description())
                .negation(this.negation())
                .title(this.title())
                .disabled(this.disabled())
                .build();
    }

    public ReferencedSearchFilter withId(String id) {
        return toBuilder().id(id).build();
    }

    @Override
    public UsedSearchFilter toNativeEntity(Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Object> nativeEntities) {
        final DBSearchFilter dbFilter = (DBSearchFilter) nativeEntities.get(EntityDescriptor.create(id(), ModelTypes.SEARCH_FILTER_V1));
        if (dbFilter != null) {
            // If this filter references a newly imported filter, update this filter with the ID of the new filter created in MongoDB.
            return this.withId(dbFilter.id());
        } else {
            // Otherwise return this filter as it is in the parent entity.
            return this;
        }
    }

    @Override
    public UsedSearchFilter toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        final Optional<String> entityId = entityDescriptorIds.get(EntityDescriptor.create(id(), ModelTypes.SEARCH_FILTER_V1));
        if(entityId.isPresent()) {
            // If this filter references a filter we are exporting,
            // update this filter with the exported filter so that we can reference the new filter created on import.
            return this.withId(entityId.get());
        } else {
            // Otherwise return this filter as it is in the parent entity.
            return this;
        }
    }

    @Override
    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        mutableGraph.putEdge(entityDescriptor, EntityDescriptor.create(id(), ModelTypes.SEARCH_FILTER_V1));
    }

    @Override
    public void resolveForInstallation(EntityV1 entity,
                                       Map<String, ValueReference> parameters,
                                       Map<EntityDescriptor, Entity> entities,
                                       MutableGraph<Entity> graph) {
        graph.putEdge(entity, entities.get(EntityDescriptor.create(id(), ModelTypes.SEARCH_FILTER_V1)));
    }
}
