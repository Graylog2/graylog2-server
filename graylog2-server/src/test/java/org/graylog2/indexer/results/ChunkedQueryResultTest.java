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
package org.graylog2.indexer.results;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.results.ChunkedQueryResultTest.ServerlessChunkedQueryResultSimulation.BACKING_RESULT_LIST;
import static org.mockito.Mockito.doReturn;


public class ChunkedQueryResultTest {

    private static final String INDEX_NAME = "graylog_0";

    private ServerlessChunkedQueryResultSimulation toTest;

    @Test
    void emptyResultWhenNextSearchResultReturnsNull() throws Exception {
        toTest = Mockito.spy(new ServerlessChunkedQueryResultSimulation("Client",
                List.of(),
                "",
                List.of("name"),
                100,
                20
        ));
        doReturn(null).when(toTest).nextSearchResult();

        final ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk).isNull();
    }

    @Test
    void emptyResultWhenLimitIsZero() throws Exception {
        toTest = new ServerlessChunkedQueryResultSimulation("Client",
                null,
                "",
                List.of("name"),
                0,
                2
        );

        final ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk).isNull();
    }

    @Test
    void getsFirstChunkIfInitialResultIsNull() throws Exception {
        toTest = new ServerlessChunkedQueryResultSimulation("Client",
                null,
                "",
                List.of("name"),
                10,
                2
        );

        final ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isTrue();
        final List<ResultMessage> messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(2);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(0));
        verifyElementAt(messages, 1, BACKING_RESULT_LIST.get(1));
    }

    @Test
    void getsFirstChunkFromInitialResult() throws Exception {
        toTest = new ServerlessChunkedQueryResultSimulation("Client",
                List.of("Alice", "Barbara"),
                "",
                List.of("name"),
                10,
                2
        );

        final ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isTrue();
        final List<ResultMessage> messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(2);
        verifyElementAt(messages, 0, "Alice");
        verifyElementAt(messages, 1, "Barbara");
    }

    @Test
    void doesNotExceedLimit() throws Exception {
        toTest = new ServerlessChunkedQueryResultSimulation("Client",
                null,
                "",
                List.of("name"),
                7,
                3
        );

        ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isTrue();
        List<ResultMessage> messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(3);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(0));
        verifyElementAt(messages, 1, BACKING_RESULT_LIST.get(1));
        verifyElementAt(messages, 2, BACKING_RESULT_LIST.get(2));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isFalse();
        messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(3);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(3));
        verifyElementAt(messages, 1, BACKING_RESULT_LIST.get(4));
        verifyElementAt(messages, 2, BACKING_RESULT_LIST.get(5));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isFalse();
        messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(1);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(6));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk).isNull();
    }

    @Test
    void stopsWhenNoMoreResults() throws Exception {
        toTest = new ServerlessChunkedQueryResultSimulation("Client",
                null,
                "",
                List.of("name"),
                777,
                4
        );

        ResultChunk resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isTrue();
        List<ResultMessage> messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(4);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(0));
        verifyElementAt(messages, 1, BACKING_RESULT_LIST.get(1));
        verifyElementAt(messages, 2, BACKING_RESULT_LIST.get(2));
        verifyElementAt(messages, 3, BACKING_RESULT_LIST.get(3));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isFalse();
        messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(4);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(4));
        verifyElementAt(messages, 1, BACKING_RESULT_LIST.get(5));
        verifyElementAt(messages, 2, BACKING_RESULT_LIST.get(6));
        verifyElementAt(messages, 3, BACKING_RESULT_LIST.get(7));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk.isFirstChunk()).isFalse();
        messages = resultChunk.messages();
        assertThat(messages)
                .isNotNull()
                .hasSize(1);
        verifyElementAt(messages, 0, BACKING_RESULT_LIST.get(8));

        resultChunk = toTest.nextChunk();
        assertThat(resultChunk).isNull();
    }

    public static class ServerlessChunkedQueryResultSimulation extends ChunkedQueryResult<String, List<String>> {

        static final List<String> BACKING_RESULT_LIST = List.of("Adam", "Bob", "Cedrick", "Donald", "Elvis", "Fred", "George", "Henry", "Ian");

        private int fromIndex;
        private final int batchSize;

        public ServerlessChunkedQueryResultSimulation(String client,
                                                      List<String> initialResult,
                                                      String query,
                                                      List<String> fields,
                                                      int limit,
                                                      int batchSize) {
            super(client, initialResult, query, fields, limit);
            this.batchSize = batchSize;
        }

        @Override
        protected List<ResultMessage> collectMessagesFromResult(List<String> result) {
            return result.stream()
                    .map(res -> ResultMessage.parseFromSource(res, INDEX_NAME, Map.of("name", res)))
                    .collect(Collectors.toList());
        }

        @Override
        @Nullable
        protected List<String> nextSearchResult() throws IOException {
            final int toIndex = Math.min(fromIndex + batchSize, BACKING_RESULT_LIST.size());
            if (fromIndex >= toIndex) {
                return List.of();
            }
            final List<String> result = BACKING_RESULT_LIST.subList(fromIndex, toIndex);
            fromIndex += batchSize;
            return result;
        }

        @Override
        protected String getChunkingMethodName() {
            return "simulation";
        }

        @Override
        protected long countTotalHits(List<String> response) {
            return BACKING_RESULT_LIST.size();
        }

        @Override
        protected long getTookMillisFromResponse(List<String> response) {
            return 42;
        }

        @Override
        public void cancel() throws IOException {

        }

    }

    private void verifyElementAt(final List<ResultMessage> messages, final int index, final String expectedValue) {
        assertThat(messages)
                .element(index)
                .isNotNull()
                .extracting(el -> el.getMessage().getField("name"))
                .isEqualTo(expectedValue);
    }
}
