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
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.AggregationBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.CountExistingBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.fieldtypes.util.TextFieldTypesSeparator;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tries to query the search engine (ES/Opensearch) to find out which fields are relevant for the streams.
 */
@Named("AdHocFilter")
public class AdHocSearchEngineStreamBasedFieldTypeFilter implements StreamBasedFieldTypeFilter {

    private final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter;
    private final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter;

    @Inject
    public AdHocSearchEngineStreamBasedFieldTypeFilter(final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter,
                                                       final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter) {
        this.aggregationBasedFieldTypeFilterAdapter = aggregationBasedFieldTypeFilterAdapter;
        this.countExistingBasedFieldTypeFilterAdapter = countExistingBasedFieldTypeFilterAdapter;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        if (streamIds == null || streamIds.isEmpty()) {
            return Collections.emptySet();
        }

        final TextFieldTypesSeparator textFieldTypesSeparator = new TextFieldTypesSeparator();
        textFieldTypesSeparator.separate(fieldTypeDTOs);

        Set<FieldTypeDTO> filtered = new HashSet<>();
        filtered.addAll(aggregationBasedFieldTypeFilterAdapter.filterFieldTypes(textFieldTypesSeparator.getNonTextFields(), indexNames, streamIds));
        filtered.addAll(countExistingBasedFieldTypeFilterAdapter.filterFieldTypes(textFieldTypesSeparator.getTextFields(), indexNames, streamIds));
        return filtered;
    }
}
