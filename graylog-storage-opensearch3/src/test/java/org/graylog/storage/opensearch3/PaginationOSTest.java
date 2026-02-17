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
package org.graylog.storage.opensearch3;

import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationOSTest {
    private static final AbsoluteRange RANGE = AbsoluteRange.create(
            DateTime.parse("2020-07-23T11:03:32.243Z"),
            DateTime.parse("2020-07-23T11:08:32.243Z")
    );

    private PaginationOS paginationOS;

    @BeforeEach
    void setUp() {
        final SearchRequestFactoryOS searchRequestFactory = new SearchRequestFactoryOS(
                true,  // allowLeadingWildcardSearches
                new IgnoreSearchFilters()
        );

        paginationOS = new PaginationOS(
                null,  // resultMessageFactory - not needed for these tests
                null,  // opensearchClient - not needed for these tests
                searchRequestFactory,
                true   // allowHighlighting
        );
    }

    @Test
    void buildSearchRequestShouldUseProvidedIndices() {
        // Given
        final Set<String> indices = Set.of("graylog_0", "graylog_1", "graylog_2");
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(indices)
                .range(RANGE)
                .fields(List.of("message", "timestamp"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.index()).containsExactlyInAnyOrder("graylog_0", "graylog_1", "graylog_2");
    }

    @Test
    void buildSearchRequestShouldUseLenientExpandOpenIndicesOptions() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.ignoreUnavailable()).isTrue();
        assertThat(request.allowNoIndices()).isFalse();
        assertThat(request.expandWildcards()).contains(ExpandWildcard.Open);
    }

    @Test
    void buildSearchRequestShouldIncludeQuery() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .query("test query")
                .fields(List.of("message", "timestamp"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.query()).isNotNull();
    }

    @Test
    void buildSearchRequestShouldHandleSingleIndex() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("single_index"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.index()).containsExactly("single_index");
    }

    @Test
    void buildSearchRequestShouldSetBatchSize() {
        // Given
        final int batchSize = 50;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .batchSize(batchSize)
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.size()).isEqualTo(batchSize);
    }

    @Test
    void buildSearchRequestShouldSetFetchSourceFields() {
        // Given
        final List<String> fields = List.of("field1", "field2", "field3");
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(fields)
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.source()).isNotNull();
        assertThat(request.source().filter()).isNotNull();
        assertThat(request.source().filter().includes())
                .containsExactlyInAnyOrder("field1", "field2", "field3");
    }

    @Test
    void buildSearchRequestShouldIncludeTimeRange() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        final String requestJson = request.toJsonString();
        assertThat(requestJson).contains("2020-07-23 11:03:32.243");
        assertThat(requestJson).contains("2020-07-23 11:08:32.243");
    }

    @Test
    void buildSearchRequestShouldSetTrackTotalHits() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.trackTotalHits()).isNotNull();
        assertThat(request.trackTotalHits().enabled()).isTrue();
    }

    @Test
    void buildSearchRequestShouldIncludeSliceParams() {
        // Given
        final ChunkCommand.SliceParams sliceParams = new ChunkCommand.SliceParams(2, 5);
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .sliceParams(sliceParams)
                .build();

        // When
        final SearchRequest request = paginationOS.buildSearchRequest(chunkCommand);

        // Then
        assertThat(request.slice()).isNotNull();
        assertThat(request.slice().id()).isEqualTo(2);
        assertThat(request.slice().max()).isEqualTo(5);
    }

    @Test
    void createQueryShouldIncludeQueryString() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .query("test query")
                .fields(List.of("message"))
                .build();

        // When
        final Query query = paginationOS.createQuery(chunkCommand);

        // Then
        assertThat(query.toJsonString()).contains("test query");
    }
}
