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
package org.graylog.storage.elasticsearch7.fieldtypes.streams;

import one.util.streamex.EntryStream;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.ExistsQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.TermsQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.CountExistingBasedFieldTypeFilterAdapter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CountExistingBasedFieldTypeFilterES7 implements CountExistingBasedFieldTypeFilterAdapter {

    private final ElasticsearchClient client;

    @Inject
    public CountExistingBasedFieldTypeFilterES7(final ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs, final Set<String> indexNames, final Collection<String> streamIds) {
        if (indexNames == null || indexNames.isEmpty()) {
            return Collections.emptySet();
        }
        if (streamIds == null || streamIds.isEmpty()) {
            return fieldTypeDTOs;
        }
        if (fieldTypeDTOs == null || fieldTypeDTOs.isEmpty()) {
            return fieldTypeDTOs;
        }

        List<FieldTypeDTO> orderedFieldTypeDTOs = new ArrayList<>(fieldTypeDTOs);
        final List<MultiSearchResponse.Item> msearchResponse = client.msearch(orderedFieldTypeDTOs.stream()
                        .map(f -> buildSearchRequestForParticularFieldExistence(f, indexNames, streamIds))
                        .collect(Collectors.toList()),
                "Unable to retrieve existing text fields types");

        final Set<FieldTypeDTO> filteredFields = EntryStream.of(orderedFieldTypeDTOs)
                .filterKeyValue((i, field) -> msearchResponse.get(i).getResponse().getHits().getTotalHits().value > 0)
                .values()
                .collect(Collectors.toSet());

        return filteredFields;
    }

    private SearchRequest buildSearchRequestForParticularFieldExistence(final FieldTypeDTO fieldType, final Set<String> indexNames, final Collection<String> streamIds) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(new BoolQueryBuilder()
                        .filter(new TermsQueryBuilder("streams", streamIds))
                        .filter(new ExistsQueryBuilder(fieldType.fieldName()))
                )
                .size(0)
                .trackTotalHitsUpTo(1)
                .terminateAfter(1);

        return new SearchRequest(indexNames.toArray(new String[0]))
                .source(searchSourceBuilder);
    }

}
