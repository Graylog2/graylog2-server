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
package org.graylog2.indexer.fieldtypes;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.engine.SearchExecutor;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscoveredFieldTypeServiceImpl implements DiscoveredFieldTypeService {

    private final SearchExecutor searchExecutor;
    private final IndexFieldTypesService indexFieldTypesService;
    private final FieldTypesMerger fieldTypesMerger;

    @Inject
    public DiscoveredFieldTypeServiceImpl(final SearchExecutor searchExecutor,
                                          final IndexFieldTypesService indexFieldTypesService,
                                          final FieldTypesMerger fieldTypesMerger) {
        this.searchExecutor = searchExecutor;
        this.indexFieldTypesService = indexFieldTypesService;
        this.fieldTypesMerger = fieldTypesMerger;
    }

    @Override
    public Set<MappedFieldTypeDTO> fieldTypesBySearch(final Search search, final SearchUser searchUser) {
        final Set<String> discoveredFields = searchExecutor.getFieldsPresentInSearchResultDocuments(search, searchUser);
        if (discoveredFields != null && !discoveredFields.isEmpty()) {
            final Collection<IndexFieldTypesDTO> forFieldNames = indexFieldTypesService.findForFieldNames(discoveredFields);
            final Collection<IndexFieldTypesDTO> withOnlyDiscoveredFieldsRetained = forFieldNames.stream()
                    .map(i -> IndexFieldTypesDTO.create(
                            i.indexSetId(),
                            i.indexName(),
                            i.fields().stream()
                                    .filter(f -> discoveredFields.contains(f.fieldName()))
                                    .collect(Collectors.toSet()))
                    )
                    .collect(Collectors.toSet());
            return fieldTypesMerger.mergeCompoundFieldTypes(withOnlyDiscoveredFieldsRetained.stream()
                    .flatMap(fieldTypes -> fieldTypes.fields().stream())
                    .map(fieldTypesMerger::mapPhysicalFieldType));
        }
        return Set.of();
    }
}
