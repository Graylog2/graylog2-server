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
package org.graylog2.indexer.fieldtypes.streamfiltered.filters;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.MissingStoredStreamFieldsException;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.StoredStreamFieldsService;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.model.StoredStreamFields;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Named("StoredFilter")
public class StoredSearchEngineStreamBasedFieldTypeFilter implements StreamBasedFieldTypeFilter {

    private final StoredStreamFieldsService storedStreamFieldsService;

    @Inject
    public StoredSearchEngineStreamBasedFieldTypeFilter(final StoredStreamFieldsService storedStreamFieldsService) {
        this.storedStreamFieldsService = storedStreamFieldsService;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        if (streamIds == null || streamIds.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<FieldTypeDTO> storedFields = new HashSet<>();
        for (String streamId : streamIds) {
            final Optional<StoredStreamFields> storedStreamFields = storedStreamFieldsService.get(streamId);
            if (!storedStreamFields.isPresent() || storedStreamFields.get().isOutdated()) {
                throw new MissingStoredStreamFieldsException(streamId);
            } else {
                final Set<FieldTypeDTO> fields = storedStreamFields.get().fields();
                if (fields != null) {
                    storedFields.addAll(fields);
                }
            }
        }

        storedFields.retainAll(fieldTypeDTOs);
        return storedFields;
    }
}
