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

import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;

import java.util.Map;

public interface ReferencedSearchFilter extends UsedSearchFilter {

    String id();

    InlineQueryStringSearchFilter toInlineRepresentation();

    ReferencedSearchFilter withId(String id);

    @Override
    default void resolveForInstallation(EntityV1 parentEntity, Map<EntityDescriptor, Entity> entities, MutableGraph<Entity> graph) {
        final Entity filterEntity = entities.get(EntityDescriptor.create(ModelId.of(id()), ModelTypes.SEARCH_FILTER_V1));
        if (filterEntity != null) {
            graph.putEdge(parentEntity, filterEntity);
        }
    }

    @Override
    default void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        mutableGraph.putEdge(entityDescriptor, EntityDescriptor.create(id(), ModelTypes.SEARCH_FILTER_V1));
    }
}
