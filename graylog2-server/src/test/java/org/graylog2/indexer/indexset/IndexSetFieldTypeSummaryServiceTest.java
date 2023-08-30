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
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.indexset.IndexSetFieldTypeSummaryService.DEFAULT_SORT_FIELD;
import static org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary.INDEX_SET_TITLE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class IndexSetFieldTypeSummaryServiceTest {

    private IndexSetFieldTypeSummaryService toTest;

    @Mock
    private IndexFieldTypesService indexFieldTypesService;
    @Mock
    private StreamService streamService;
    @Mock
    private IndexSetService indexSetService;

    @BeforeEach
    void setUp() {
        toTest = new IndexSetFieldTypeSummaryService(indexFieldTypesService, streamService, indexSetService);
    }

    @Test
    void testDoesNotReturnResultsForIndexSetsIfUserMissesPriviledges() {
        Predicate<String> indexSetPermissionPredicateAlwaysReturningFalse = x -> false;
        doReturn(Set.of("index_set_id")).when(streamService).indexSetIdsByIds(Set.of("stream_id"));
        final PageListResponse<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("stream_id"), "field_name", indexSetPermissionPredicateAlwaysReturningFalse);

        assertThat(summary.elements()).isEmpty();
        verifyNoInteractions(indexFieldTypesService);
        verifyNoMoreInteractions(streamService);
        verifyNoInteractions(indexSetService);
    }

    @Test
    void testDoesNotReturnResultsForIndexSetsIfItDoesNotExist() {
        Predicate<String> indexSetPermissionPredicateAlwaysReturningFalse = x -> false;
        doReturn(Set.of("index_set_id")).when(streamService).indexSetIdsByIds(Set.of("stream_id"));
        final PageListResponse<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("stream_id"), "field_name", indexSetPermissionPredicateAlwaysReturningFalse);
        assertThat(summary.elements()).isEmpty();
        verifyNoInteractions(indexFieldTypesService);
        verifyNoMoreInteractions(streamService);
    }

    @Test
    void testFillsSummaryDataProperly() {
        Predicate<String> indexSetPermissionPredicate = indexSetID -> indexSetID.contains("canSee");
        doReturn(Set.of("canSee", "cannotSee")).when(streamService).indexSetIdsByIds(Set.of("stream_id"));

        doReturn(List.of("Stream1", "Stream2")).when(streamService).streamTitlesForIndexSet("canSee");
        doReturn(List.of("text", "keyword")).when(indexFieldTypesService).fieldTypeHistory("canSee", "field_name", true);
        mockIndexSetConfig("canSee", "Index Set From The Top Of The Universe");

        final PageListResponse<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("stream_id"), "field_name", indexSetPermissionPredicate);
        assertThat(summary.elements())
                .isNotNull()
                .isEqualTo(List.of(new IndexSetFieldTypeSummary("canSee", "Index Set From The Top Of The Universe", List.of("Stream1", "Stream2"), List.of("text", "keyword"))));
    }

    @Test
    void testComplexPaginationScenario() {
        Predicate<String> indexSetPermissionPredicate = indexSetID -> indexSetID.contains("canSee");
        final Set<String> allStreams = Set.of("stream_id_1", "stream_id_2", "stream_id_3", "stream_id_4", "stream_id_5");
        doReturn(Set.of("canSee1", "cannotSee", "canSeeButDoesNotExist", "canSee2", "canSee3"))
                .when(streamService)
                .indexSetIdsByIds(allStreams);

        doReturn(List.of("Stream1", "Stream2")).when(streamService).streamTitlesForIndexSet("canSee1");
        doReturn(List.of("Stream2")).when(streamService).streamTitlesForIndexSet("canSee2");
        doReturn(List.of("Stream3")).when(streamService).streamTitlesForIndexSet("canSee3");

        doReturn(List.of("text", "keyword")).when(indexFieldTypesService).fieldTypeHistory("canSee1", "field_name", true);
        doReturn(List.of("text")).when(indexFieldTypesService).fieldTypeHistory("canSee2", "field_name", true);
        doReturn(List.of()).when(indexFieldTypesService).fieldTypeHistory("canSee3", "field_name", true);

        mockIndexSetConfig("canSee1", "Aa");
        mockIndexSetConfig("canSee2", "Ab");
        mockIndexSetConfig("canSee3", "Z");
        doReturn(Optional.empty()).when(indexSetService).get("canSeeButDoesNotExist");

        final PageListResponse<IndexSetFieldTypeSummary> allResultsOnSinglePageSortedByIdAsc = toTest.getIndexSetFieldTypeSummary(allStreams, "field_name", indexSetPermissionPredicate,
                1, 5, DEFAULT_SORT_FIELD, Sorting.Direction.ASC);
        assertThat(allResultsOnSinglePageSortedByIdAsc.elements())
                .isNotNull()
                .isEqualTo(List.of(
                        new IndexSetFieldTypeSummary("canSee1", "Aa", List.of("Stream1", "Stream2"), List.of("text", "keyword")),
                        new IndexSetFieldTypeSummary("canSee2", "Ab", List.of("Stream2"), List.of("text")),
                        new IndexSetFieldTypeSummary("canSee3", "Z", List.of("Stream3"), List.of())
                ));
        assertThat(allResultsOnSinglePageSortedByIdAsc.total()).isEqualTo(3);
        assertThat(allResultsOnSinglePageSortedByIdAsc.paginationInfo().count()).isEqualTo(3);

        final PageListResponse<IndexSetFieldTypeSummary> thirdSingleElemPageWithSortByIdDesc = toTest.getIndexSetFieldTypeSummary(allStreams, "field_name", indexSetPermissionPredicate,
                3, 1, DEFAULT_SORT_FIELD, Sorting.Direction.DESC);
        assertThat(thirdSingleElemPageWithSortByIdDesc.elements())
                .isNotNull()
                .isEqualTo(List.of(
                        new IndexSetFieldTypeSummary("canSee1", "Aa", List.of("Stream1", "Stream2"), List.of("text", "keyword"))
                ));
        assertThat(thirdSingleElemPageWithSortByIdDesc.total()).isEqualTo(3);
        assertThat(thirdSingleElemPageWithSortByIdDesc.paginationInfo().count()).isEqualTo(1);

        final PageListResponse<IndexSetFieldTypeSummary> firstTwoElemPageWithSortByTitleDesc = toTest.getIndexSetFieldTypeSummary(allStreams, "field_name", indexSetPermissionPredicate,
                1, 2, INDEX_SET_TITLE, Sorting.Direction.DESC);
        assertThat(firstTwoElemPageWithSortByTitleDesc.elements())
                .isNotNull()
                .isEqualTo(List.of(
                        new IndexSetFieldTypeSummary("canSee3", "Z", List.of("Stream3"), List.of()),
                        new IndexSetFieldTypeSummary("canSee2", "Ab", List.of("Stream2"), List.of("text"))
                ));
        assertThat(firstTwoElemPageWithSortByTitleDesc.total()).isEqualTo(3);
        assertThat(firstTwoElemPageWithSortByTitleDesc.paginationInfo().count()).isEqualTo(2);

    }

    private void mockIndexSetConfig(final String id, final String title) {
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn(title).when(indexSetConfig).title();
        doReturn(id).when(indexSetConfig).id();
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get(id);
    }
}
