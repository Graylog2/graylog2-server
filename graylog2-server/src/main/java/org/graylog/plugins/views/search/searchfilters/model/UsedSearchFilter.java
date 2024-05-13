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
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
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
public interface UsedSearchFilter extends NativeEntityConverter<UsedSearchFilter>, ContentPackable<UsedSearchFilter> {

    String TYPE = "type";

    String TITLE_FIELD = "title";
    String ID_FIELD = "id";
    String DESCRIPTION_FIELD = "description";
    String QUERY_STRING_FIELD = "queryString";
    String NEGATION_FIELD = "negation";
    String DISABLED_FIELD = "disabled";

    String INLINE_QUERY_STRING_SEARCH_FILTER = "inlineQueryString";
    String REFERENCED_SEARCH_FILTER = "referenced";

    boolean negation();

    boolean disabled();

    // This method should only be overridden for referenced filters as those will be exported as separate entities in content packs.
    // Inline filters will not be updated during import/export.
    @Override
    default UsedSearchFilter toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return this;
    }

    // This method should only be overridden for referenced filters as those will be exported as separate entities in content packs.
    // Inline filters will not be updated during import/export.
    @Override
    default UsedSearchFilter toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return this;
    }
}
