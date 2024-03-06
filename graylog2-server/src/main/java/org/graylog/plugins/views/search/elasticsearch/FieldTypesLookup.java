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
package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.Sets;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;

import jakarta.inject.Inject;
import org.graylog2.streams.StreamService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldTypesLookup {
    private final IndexFieldTypesService indexFieldTypesService;
    private final StreamService streamService;

    @Inject
    public FieldTypesLookup(IndexFieldTypesService indexFieldTypesService, StreamService streamService) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.streamService = streamService;
    }

    private Map<String, Set<String>> get(Set<String> streamIds) {
        return findForStreamIds(streamIds)
                .stream()
                .flatMap(indexFieldTypes -> indexFieldTypes.fields().stream())
                .collect(Collectors.toMap(
                        FieldTypeDTO::fieldName,
                        fieldType -> Collections.singleton(fieldType.physicalType()),
                        Sets::union
                ));
    }

    private Collection<IndexFieldTypesDTO> findForStreamIds(Collection<String> streamIds) {
        final Set<String> indexSetIds = getIndexSetIds(streamIds);
        return indexFieldTypesService.findForIndexSets(indexSetIds);
    }

    @NotNull
    private Set<String> getIndexSetIds(Collection<String> streamIds) {
        return streamService.loadByIds(streamIds)
                .stream()
                .filter(Objects::nonNull)
                .map(stream -> stream.getIndexSet().getConfig().id())
                .collect(Collectors.toSet());
    }

    public Optional<String> getType(Set<String> streamIds, String field) {
        final Map<String, Set<String>> allFieldTypes = this.get(streamIds);
        final Set<String> fieldTypes = allFieldTypes.get(field);
        return typeFromFieldType(fieldTypes);
    }

    /**
     * It may be inefficient to call getType() method for multiple fields, if the list of streams does not change.
     * In that scenario this method will be better, hitting Mongo DB only once.
     */
    public Map<String, String> getTypes(final Set<String> streamIds,
                                        final Set<String> fields) {
        final Map<String, Set<String>> allFieldTypes = this.get(streamIds);

        final Map<String, String> result = new HashMap<>(fields.size());
        fields.forEach(field -> {
            final Set<String> fieldTypes = allFieldTypes.get(field);
            typeFromFieldType(fieldTypes).ifPresent(s -> result.put(field, s));
        });

        return result;
    }

    private Optional<String> typeFromFieldType(Set<String> fieldTypes) {
        return fieldTypes == null || fieldTypes.size() > 1 ? Optional.empty() : fieldTypes.stream().findFirst();
    }
}
