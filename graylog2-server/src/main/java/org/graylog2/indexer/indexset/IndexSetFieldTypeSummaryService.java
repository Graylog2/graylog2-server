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
package org.graylog2.indexer.indexset;

import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class IndexSetFieldTypeSummaryService {

    private final IndexFieldTypesService indexFieldTypesService;
    private final StreamService streamService;
    private final IndexSetService indexSetService;

    @Inject
    public IndexSetFieldTypeSummaryService(final IndexFieldTypesService indexFieldTypesService,
                                           final StreamService streamService,
                                           final IndexSetService indexSetService) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.streamService = streamService;
        this.indexSetService = indexSetService;
    }

    public List<IndexSetFieldTypeSummary> getIndexSetFieldTypeSummary(final Set<String> streamIds,
                                                                      final String fieldName,
                                                                      final Predicate<String> indexSetPermissionPredicate) {
        //There is a potential to improve performance by introducing a complicated aggregation pipeline joining multiple Mongo collections, especially if permission restrictions were simplified.
        //For now simpler solution has been used in the implementation.
        List<IndexSetFieldTypeSummary> response = new LinkedList<>();
        final Set<String> indexSetsIds = streamService.indexSetIdsByIds(streamIds);
        for (String indexSetId : indexSetsIds) {
            if (indexSetPermissionPredicate.test(indexSetId)) {
                final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(indexSetId);
                if (indexSetConfig.isPresent()) {
                    response.add(new IndexSetFieldTypeSummary(indexSetId,
                            indexSetConfig.map(IndexSetConfig::title).orElse(""),
                            streamService.streamTitlesForIndexSet(indexSetId),
                            indexFieldTypesService.fieldTypeHistory(indexSetId, fieldName, true)));
                }
            }
        }

        return response;
    }
}
