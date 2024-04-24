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

import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface UsesSearchFilters {
    List<UsedSearchFilter> filters();

    default List<UsedSearchFilter> exportFilters(EntityDescriptorIds entityDescriptorIds) {
        final List<UsedSearchFilter> filters = new ArrayList<>();
        for (UsedSearchFilter filter : filters()) {
            UsedSearchFilter toUse = filter;
            if (filter instanceof ReferencedSearchFilter referencedFilter) {
                final String entityId = entityDescriptorIds.get(EntityDescriptor.create(referencedFilter.id(), ModelTypes.SEARCH_FILTER_V1))
                        .orElse(referencedFilter.id());
                if(entityId != null) {
                    toUse = referencedFilter.withId(entityId);
                }
            }
            filters.add(toUse);
        }
        return filters;
    }

    static List<UsedSearchFilter> createNativeFilters(List<UsedSearchFilter> usedFilters, Map<EntityDescriptor, Object> nativeEntities) {
        final List<UsedSearchFilter> filters = new ArrayList<>();
        for (UsedSearchFilter filter : usedFilters) {
            UsedSearchFilter toUse = filter;
            if (filter instanceof ReferencedSearchFilter referencedFilter) {
                final DBSearchFilter dbFilter = (DBSearchFilter) nativeEntities.get(EntityDescriptor.create(referencedFilter.id(), ModelTypes.SEARCH_FILTER_V1));
                if (dbFilter != null) {
                    toUse = referencedFilter.withId(dbFilter.id());
                }
            }
            filters.add(toUse);
        }
        return filters;
    }
}
