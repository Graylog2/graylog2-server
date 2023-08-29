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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
        final List<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("id"), "field_name", indexSetPermissionPredicateAlwaysReturningFalse);
        assertThat(summary).isEmpty();
        verifyNoInteractions(indexFieldTypesService);
        verifyNoInteractions(streamService);
        verifyNoInteractions(indexSetService);
    }

    @Test
    void testDoesNotReturnResultsForIndexSetsIfItDoesNotExist() {
        Predicate<String> indexSetPermissionPredicateAlwaysReturningFalse = x -> false;
        final List<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("id"), "field_name", indexSetPermissionPredicateAlwaysReturningFalse);
        assertThat(summary).isEmpty();
        verifyNoInteractions(indexFieldTypesService);
        verifyNoInteractions(streamService);
    }

    @Test
    void testFillsSummaryDataProperly() {
        Predicate<String> indexSetPermissionPredicate = indexSetID -> indexSetID.contains("canSee");

        doReturn(Set.of("Stream1", "Stream2")).when(streamService).streamTitlesForIndexSet("canSee");
        doReturn(List.of("text", "keyword")).when(indexFieldTypesService).fieldTypeHistory("canSee", "field_name", true);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn("Index Set From The Top Of The Universe").when(indexSetConfig).title();
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get("canSee");

        final List<IndexSetFieldTypeSummary> summary = toTest.getIndexSetFieldTypeSummary(Set.of("canSee", "cannotSee"), "field_name", indexSetPermissionPredicate);
        assertThat(summary)
                .isNotNull()
                .isEqualTo(List.of(new IndexSetFieldTypeSummary("canSee", "Index Set From The Top Of The Universe", Set.of("Stream1", "Stream2"), List.of("text", "keyword"))));
    }
}
