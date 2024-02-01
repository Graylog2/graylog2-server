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
package org.graylog2.periodical;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS)
public class IndexRangesCleanUpIT {
    public static final String RANGE_CLEANUP_PREFIX = "range-cleanup";
    public static final String INDEX_TWO = RANGE_CLEANUP_PREFIX + "_1";
    public static final String INDEX_ONE = RANGE_CLEANUP_PREFIX + "_0";
    private final GraylogApis api;

    public IndexRangesCleanUpIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void testCleanUp() throws ExecutionException, RetryException {
        String indexSetId = api.indices().createIndexSet("Range clean up", "test index range clean up", RANGE_CLEANUP_PREFIX);

        //Rotate to create indices 0 & 1
        api.indices().rotateIndexSet(indexSetId);
        api.indices().rotateIndexSet(indexSetId);

        assertThat(getIndexRangesList()).isNotEmpty().contains(INDEX_ONE, INDEX_TWO);

        //Deleting index should automatically remove the range
        api.indices().deleteIndex(INDEX_ONE);

        assertThat(getIndexRangesList()).isNotEmpty().doesNotContain(INDEX_ONE);

        //Deleting index set without deleting underlying indices
        api.indices().deleteIndexSet(indexSetId, false);
        assertThat(getIndexRangesList()).isNotEmpty().contains(INDEX_TWO);

        //Trigger clean up periodical over api
        api.indices().rebuildIndexRanges();
        assertThat(getIndexRangesList()).isNotEmpty().doesNotContain(INDEX_TWO);
    }

    private List<String> getIndexRangesList() throws ExecutionException, RetryException {
        return RetryerBuilder.<List<String>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .retryIfResult(List::isEmpty)
                .build()
                .call(() -> api.indices().listIndexRanges().properJSONPath().read("ranges.*.index_name"));
    }
}
