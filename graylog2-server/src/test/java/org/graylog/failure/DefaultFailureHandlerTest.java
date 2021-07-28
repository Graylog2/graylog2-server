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
package org.graylog.failure;

import com.google.common.collect.ImmutableList;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class DefaultFailureHandlerTest {

    private final IndexFailureService indexFailureService = mock(IndexFailureService.class);
    private final DefaultFailureHandler underTest = new DefaultFailureHandler(indexFailureService);

    @Test
    public void isEnabled_returnsTrue() {
        assertThat(underTest.isEnabled()).isTrue();
    }

    @Test
    public void supports_indexingFailuresSupported() {
        assertThat(underTest.supports(FailureBatch.indexingFailureBatch(new ArrayList<>()))).isTrue();
    }

    @Test
    public void supports_indexingFailuresNotSupported() {
        assertThat(underTest.supports(FailureBatch.indexingFailureBatch(new ArrayList<>()))).isTrue();
    }

    @Test
    public void supports_processingFailuresNotSupported() {
        assertThat(underTest.supports(FailureBatch.processingFailureBatch(new ArrayList<>()))).isFalse();
    }

    @Test
    public void handle_allFailuresHandedOverToIndexFailureService() {
        // given
        final DateTime ts = DateTime.now(DateTimeZone.UTC);
        final Indexable indexable1 = mock(Indexable.class);
        final Indexable indexable2 = mock(Indexable.class);

        final IndexingFailure indexingFailure1 = new IndexingFailure(
                "indexingFailure1", "index1",
                "indexing", "Failure Error #1", ts, indexable1);

        final IndexingFailure indexingFailure2 = new IndexingFailure(
                "indexingFailure2", "index2",
                "indexing", "Failure Error #2", ts, indexable2);

        final FailureBatch indexingFailureBatch = FailureBatch.indexingFailureBatch(ImmutableList.of(indexingFailure1, indexingFailure2));

        // when
        underTest.handle(indexingFailureBatch);

        // then
        verify(indexFailureService, times(2)).saveWithoutValidation(any());

        verify(indexFailureService, times(1)).saveWithoutValidation(argThat(arg ->
                arg.asMap().get("letter_id").equals("indexingFailure1") &&
                        arg.asMap().get("index").equals("index1") &&
                        arg.asMap().get("type").equals("indexing") &&
                        arg.asMap().get("message").equals("Failure Error #1") &&
                        arg.asMap().get("timestamp") != null
                ));

        verify(indexFailureService, times(1)).saveWithoutValidation(argThat(arg ->
                arg.asMap().get("letter_id").equals("indexingFailure2") &&
                        arg.asMap().get("index").equals("index2") &&
                        arg.asMap().get("type").equals("indexing") &&
                        arg.asMap().get("message").equals("Failure Error #2") &&
                        arg.asMap().get("timestamp") != null
        ));
    }
}
