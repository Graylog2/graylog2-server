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

import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary;
import org.graylog2.streams.StreamService;

import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary.FIELD_TYPE_HISTORY;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary.INDEX_SET_ID;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary.INDEX_SET_TITLE;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary.STREAM_TITLES;

public class IndexSetFieldTypeSummaryService {

    public static final String DEFAULT_SORT_FIELD = INDEX_SET_ID;
    private static final Sorting DEFAULT_SORT = Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.ASC);
    private static final EntityDefaults ENTITY_DEFAULTS = EntityDefaults.builder()
            .sort(DEFAULT_SORT)
            .build();

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(INDEX_SET_ID).title("Index Set Id").hidden(true).sortable(true).build(),
            EntityAttribute.builder().id(INDEX_SET_TITLE).title("Index Set Title").sortable(true).build(),
            EntityAttribute.builder().id(STREAM_TITLES).title("Stream Titles").sortable(false).build(),
            EntityAttribute.builder().id(FIELD_TYPE_HISTORY).title("Current Types").sortable(false).build()
    );

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

    public PageListResponse<IndexSetFieldTypeSummary> getIndexSetFieldTypeSummary(final Set<String> streamIds,
                                                                                  final String fieldName,
                                                                                  final Predicate<String> indexSetPermissionPredicate) {
        return getIndexSetFieldTypeSummary(streamIds, fieldName, indexSetPermissionPredicate, 1, 50, DEFAULT_SORT.id(), DEFAULT_SORT.direction());
    }

    public PageListResponse<IndexSetFieldTypeSummary> getIndexSetFieldTypeSummary(final Set<String> streamIds,
                                                                                  final String fieldName,
                                                                                  final Predicate<String> indexSetPermissionPredicate,
                                                                                  final int page,
                                                                                  final int perPage,
                                                                                  final String sort,
                                                                                  final Sorting.Direction order) {
        //There is a potential to improve performance by introducing a complicated aggregation pipeline joining multiple Mongo collections, especially if permission restrictions were simplified or sorting not necessary.
        //For now simpler solution has been used in the implementation.
        final Set<String> indexSetsIds = streamService.indexSetIdsByIds(streamIds);
        List<IndexSetFieldTypeSummary> pageWithIndexSetsSummaries = indexSetsIds.stream()
                .filter(indexSetPermissionPredicate)
                .map(indexSetService::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(getComparator(sort, order))
                .skip((long) (page - 1) * perPage)
                .limit(perPage)
                .map(indexSetConfig -> {
                    String indexSetId = indexSetConfig.id();
                    return new IndexSetFieldTypeSummary(indexSetId,
                            indexSetConfig.title(),
                            streamService.streamTitlesForIndexSet(indexSetId),
                            indexFieldTypesService.fieldTypeHistory(indexSetId, fieldName, true));
                }).collect(Collectors.toList());

        long total = indexSetsIds.stream()
                .filter(indexSetPermissionPredicate)
                .map(indexSetService::get)
                .filter(Optional::isPresent)
                .count();

        return PageListResponse.create("",
                PaginatedList.PaginationInfo.create(
                        (int) total,
                        pageWithIndexSetsSummaries.size(),
                        page,
                        perPage),
                total,
                sort,
                order.toString().toLowerCase(Locale.ROOT),
                pageWithIndexSetsSummaries,
                ATTRIBUTES,
                ENTITY_DEFAULTS);

    }

    private Comparator<IndexSetConfig> getComparator(final String sort,
                                                     final Sorting.Direction order) {
        final Comparator<IndexSetConfig> comparator = Comparator.comparing(config -> {
            if (sort.equals(INDEX_SET_TITLE)) {
                return config.title();
            }
            return config.id();
        });
        if (order == Sorting.Direction.DESC) {
            return comparator.reversed();
        } else {
            return comparator;
        }
    }
}
