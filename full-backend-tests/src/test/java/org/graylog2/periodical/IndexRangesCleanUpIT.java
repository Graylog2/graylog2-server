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
import org.assertj.core.api.ListAssert;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS)
public class IndexRangesCleanUpIT {
    public static final String RANGE_CLEANUP_PREFIX = "range-cleanup";
    public static final String INDEX_TWO = RANGE_CLEANUP_PREFIX + "_1";
    public static final String INDEX_ONE = RANGE_CLEANUP_PREFIX + "_0";
    private static GraylogApis api;

    @BeforeAll
    static void init(GraylogApis graylogApis) {
        api = graylogApis;
    }

    @ContainerMatrixTest
    void testCleanUp() throws ExecutionException, RetryException {
        String indexSetId = api.indices().createIndexSet("Range clean up", "test index range clean up", RANGE_CLEANUP_PREFIX);

        //Rotate to create indices 0 & 1
        api.indices().rotateIndexSet(indexSetId);
        api.indices().rotateIndexSet(indexSetId);

        assertIndexRanges(ranges -> ranges.isNotEmpty().contains(INDEX_ONE, INDEX_TWO));

        //Deleting index should automatically remove the range
        api.indices().deleteIndex(INDEX_ONE);

        assertIndexRanges(ranges -> ranges.isNotEmpty().doesNotContain(INDEX_ONE));

        //Deleting index set without deleting underlying indices
        api.indices().deleteIndexSet(indexSetId, false);
        assertIndexRanges(ranges -> ranges.isNotEmpty().contains(INDEX_TWO));

        //Trigger clean up periodical over api
        api.indices().rebuildIndexRanges();
        assertIndexRanges(ranges -> ranges.isNotEmpty().doesNotContain(INDEX_TWO));
    }

    private void assertIndexRanges(Consumer<ListAssert<String>> assertion) throws ExecutionException, RetryException {
        RetryerBuilder.<Void>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(30, TimeUnit.SECONDS))
                .retryIfRuntimeException()
                .build()
                .call(() -> {
                    final List<String> ranges = api.indices().listIndexRanges().properJSONPath().read("ranges.*.index_name");
                    assertion.accept(assertThat(ranges));
                    return null;
                });
    }
}
