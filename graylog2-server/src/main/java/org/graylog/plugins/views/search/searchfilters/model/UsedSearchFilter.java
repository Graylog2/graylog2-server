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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.graph.Graph;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = UsedSearchFilter.TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = InlineQueryStringSearchFilter.class, name = UsedSearchFilter.INLINE_QUERY_STRING_SEARCH_FILTER),
        @JsonSubTypes.Type(value = ReferencedQueryStringSearchFilter.class, name = UsedSearchFilter.REFERENCED_SEARCH_FILTER),
})
public interface UsedSearchFilter {

    String TYPE = "type";

    String TITLE_FIELD = "title";
    String ID_FIELD = "id";
    String DESCRIPTION_FIELD = "description";
    String QUERY_STRING_FIELD = "queryString";
    String NEGATION_FIELD = "negation";
    String DISABLED_FIELD = "disabled";

    String INLINE_QUERY_STRING_SEARCH_FILTER = "inlineQueryString";
    String REFERENCED_SEARCH_FILTER = "referenced";

    //String id();

    boolean negation();

    boolean disabled();

    default void resolveForInstallation(EntityV1 parentEntity, Map<EntityDescriptor, Entity> entities, MutableGraph<Entity> graph) {
    }

    default void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {

    }
}
