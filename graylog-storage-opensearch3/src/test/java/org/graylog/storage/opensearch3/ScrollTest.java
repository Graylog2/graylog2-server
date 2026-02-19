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

class ScrollTest {
    private static final AbsoluteRange RANGE = AbsoluteRange.create(
            DateTime.parse("2020-07-23T11:03:32.243Z"),
            DateTime.parse("2020-07-23T11:08:32.243Z")
    );

    private Scroll scroll;
    private SearchRequestFactoryOS searchRequestFactory;

    @BeforeEach
    void setUp() {
        searchRequestFactory = new SearchRequestFactoryOS(
                true,  // allowLeadingWildcardSearches
                new IgnoreSearchFilters()
        );

        scroll = new Scroll(
                null,  // opensearchClient - not needed for buildScrollRequest tests
                null,  // scrollResultFactory - not needed for buildScrollRequest tests
                searchRequestFactory
        );
    }

    @Test
    void buildScrollRequestShouldUseProvidedIndices() {
        // Given
        final Set<String> indices = Set.of("graylog_0", "graylog_1", "graylog_2");
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(indices)
                .range(RANGE)
                .fields(List.of("message", "timestamp"))
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.index()).containsExactlyInAnyOrder("graylog_0", "graylog_1", "graylog_2");
    }

    @Test
    void buildScrollRequestShouldSetScrollTime() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.scroll()).isNotNull();
        assertThat(request.scroll().time()).isEqualTo("1m");
    }

    @Test
    void buildScrollRequestShouldSetIgnoreUnavailable() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.ignoreUnavailable()).isTrue();
        assertThat(request.allowNoIndices()).isTrue();
        assertThat(request.expandWildcards()).contains(ExpandWildcard.Open);
    }

    @Test
    void buildScrollRequestShouldSetBatchSize() {
        // Given
        final int batchSize = 50;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .batchSize(batchSize)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.size()).isEqualTo(batchSize);
    }

    @Test
    void buildScrollRequestShouldSetFetchSourceFields() {
        // Given
        final List<String> fields = List.of("field1", "field2", "field3");
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(fields)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.source()).isNotNull();
        assertThat(request.source().filter()).isNotNull();
        assertThat(request.source().filter().includes())
                .containsExactlyInAnyOrder("field1", "field2", "field3");
    }

    @Test
    void buildScrollRequestShouldSetTrackTotalHits() {
        // Given
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.trackTotalHits()).isNotNull();
        assertThat(request.trackTotalHits().enabled()).isTrue();
    }

    @Test
    void buildScrollRequestShouldApplyOffset() {
        // Given
        final int offset = 100;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .offset(offset)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.from()).isEqualTo(offset);
    }

    @Test
    void buildScrollRequestShouldUseLimitAsFallbackWhenBatchSizeAbsent() {
        // Given
        final int limit = 75;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .limit(limit)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.size()).isEqualTo(limit);
    }

    @Test
    void buildScrollRequestShouldPreferBatchSizeOverLimit() {
        // Given
        final int batchSize = 50;
        final int limit = 100;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .batchSize(batchSize)
                .limit(limit)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.size()).isEqualTo(batchSize);
    }

    @Test
    void buildScrollRequestShouldApplyOffsetAndLimit() {
        // Given
        final int offset = 50;
        final int limit = 25;
        final ChunkCommand chunkCommand = ChunkCommand.builder()
                .indices(Set.of("graylog_0"))
                .range(RANGE)
                .fields(List.of("message"))
                .offset(offset)
                .limit(limit)
                .build();

        final Query query = searchRequestFactory.createQuery(chunkCommand.query(), chunkCommand.range(), chunkCommand.filter());

        // When
        final SearchRequest request = scroll.buildScrollRequest(query, chunkCommand);

        // Then
        assertThat(request.from()).isEqualTo(offset);
        assertThat(request.size()).isEqualTo(limit);
    }
}
