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

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.TermsQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.AggregationBasedFieldTypeFilterAdapter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AggregationBasedFieldTypeFilterES7 implements AggregationBasedFieldTypeFilterAdapter {

    private final ElasticsearchClient client;

    @Inject
    public AggregationBasedFieldTypeFilterES7(final ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Set<FieldTypeDTO> filterFieldTypes(final Set<FieldTypeDTO> fieldTypeDTOs,
                                              final Set<String> indexNames,
                                              final Collection<String> streamIds) {
        if (indexNames == null || indexNames.isEmpty()) {
            return Collections.emptySet();
        }
        if (streamIds == null || streamIds.isEmpty()) {
            return fieldTypeDTOs;
        }
        if (fieldTypeDTOs == null || fieldTypeDTOs.isEmpty()) {
            return fieldTypeDTOs;
        }

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(new BoolQueryBuilder()
                        .filter(new TermsQueryBuilder("streams", streamIds))
                )
                .size(0);

        addAggregationsToSearch(fieldTypeDTOs, searchSourceBuilder);

        final SearchRequest searchRequest = new SearchRequest(indexNames.toArray(new String[0]))
                .source(searchSourceBuilder);

        final SearchResponse searchResult = client.search(searchRequest, "Unable to retrieve fields types aggregations");

        return fieldTypeDTOs.stream()
                .filter(fieldTypeDTO -> {
                            final Aggregation aggregation = searchResult.getAggregations().get(fieldTypeDTO.fieldName());
                            if (aggregation instanceof MultiBucketsAggregation) {
                                final List<? extends MultiBucketsAggregation.Bucket> buckets = ((MultiBucketsAggregation) aggregation).getBuckets();
                                return buckets != null && !buckets.isEmpty();
                            }
                            return false;
                        }
                ).collect(Collectors.toSet());
    }

    private void addAggregationsToSearch(final Set<FieldTypeDTO> fieldTypeDTOs, SearchSourceBuilder searchSourceBuilder) {
        for (FieldTypeDTO fieldTypeDTO : fieldTypeDTOs) {
            searchSourceBuilder.aggregation(AggregationBuilders
                    .terms(fieldTypeDTO.fieldName())
                    .field(fieldTypeDTO.fieldName() + (fieldTypeDTO.physicalType().equals("text") ? ".keyword" : "")) //should not happen, but if someone intentionally added text field here, we assume it has a corresponding keyword subfield
                    .size(1)
                    .shardSize(1)
            );
        }
    }

}
