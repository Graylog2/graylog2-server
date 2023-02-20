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
package org.graylog2.search;

import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DbFieldMappingCreator {

    public static Map<String, SearchQueryField> createFromEntityAttributes(final List<EntityAttribute> attributes) {
        Map<String, SearchQueryField> dbFieldMapping = new HashMap<>();
        attributes.stream()
                .filter(attr -> Objects.nonNull(attr.searchable()))
                .filter(EntityAttribute::searchable)
                .forEach(attr -> {
                    final SearchQueryField searchQueryField = SearchQueryField.create(
                            attr.id(),
                            attr.type()
                    );
                    dbFieldMapping.put(attr.id(), searchQueryField);
                    if (!attr.title().contains(" ")) {
                        dbFieldMapping.put(attr.title().toLowerCase(Locale.ROOT), searchQueryField);
                    }
                });
        return dbFieldMapping;
    }
}
