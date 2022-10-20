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
package org.graylog2.indexer.fieldtypes;

import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class StreamsForFieldRetrieverIT extends ElasticsearchBaseTest {

    private static final String INDEX_NAME = "graylog_0";
    private static final String STREAM_1 = "000000000000000000000001";
    private static final String STREAM_2 = "000000000000000000000002";

    protected abstract StreamsForFieldRetriever getRetriever();

    @Before
    public void setUp() {
        importFixture("org/graylog2/indexer/fieldtypes/streams/StreamsForFieldRetrieverIT.json");
    }

    @Test
    public void retrievesProperStreamsForSingleField() {
        Set<String> streams = getRetriever().getStreams("stream1_only", INDEX_NAME);
        assertThat(streams).isNotNull().hasSize(1).contains(STREAM_1);

        streams = getRetriever().getStreams("stream2_only", INDEX_NAME);
        assertThat(streams).isNotNull().hasSize(1).contains(STREAM_2);

        streams = getRetriever().getStreams("message", INDEX_NAME);
        assertThat(streams).isNotNull().hasSize(2).contains(STREAM_1, STREAM_2);

        streams = getRetriever().getStreams("additional_field", INDEX_NAME);
        assertThat(streams).isNotNull().hasSize(2).contains(STREAM_1, STREAM_2);
    }

    @Test
    public void retrievesProperStreamsForMultipleField() {
        final Map<String, Set<String>> streams = getRetriever().getStreams(List.of("stream1_only", "stream2_only", "message", "additional_field"), INDEX_NAME);
        assertThat(streams)
                .isNotNull()
                .hasSize(4)
                .hasEntrySatisfying("stream1_only", set -> assertThat(set).containsOnly(STREAM_1))
                .hasEntrySatisfying("stream2_only", set -> assertThat(set).containsOnly(STREAM_2))
                .hasEntrySatisfying("message", set -> assertThat(set).containsOnly(STREAM_1, STREAM_2))
                .hasEntrySatisfying("additional_field", set -> assertThat(set).containsOnly(STREAM_1, STREAM_2));


    }
}
