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
package org.graylog.plugins.views.aggregations;

import com.github.rholder.retry.RetryException;
import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;
import static org.hamcrest.Matchers.is;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2_LATEST})
public class AggregationSortingIT {
    private final GraylogApis api;
    private PortBoundGelfInputApi gelfInput;

    public AggregationSortingIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    void setUp() {
        this.gelfInput = api.gelf().createGelfHttpInput();
    }

    @ContainerMatrixTest
    void sortingOnNumericPivotFieldSortsNumerically() throws ExecutionException, RetryException {
        final var numericField = "numeric_field";
        final var nonNumericField = "non_numeric_field";
        try (final var env = createEnvironment()) {
            for (final var value : Set.of(9, 8, 4, 25, 2, 15, 1)) {
                env.ingestMessage(Map.of(
                        nonNumericField, "foo",
                        numericField, value,
                        "short_message", "sorting on numeric pivot test " + value
                ));
            }

            final var pivotBuilder = Pivot.builder()
                    .rowGroups(Values.builder()
                            .fields(List.of(nonNumericField, numericField)).limit(10).build())
                    .series(List.of())
                    .rollup(false);

            api.search().waitForMessage("sorting on numeric pivot test 1");

            env.waitForFieldTypes(numericField);

            final var resultDesc = env.executePivot(
                            pivotBuilder
                                    .sort(PivotSort.create(numericField, SortSpec.Direction.Descending))
                                    .build()
            );
            assertThat(resultDesc).isNotNull();

            expectKeys(resultDesc, "25", "15", "9", "8", "4", "2", "1");

            final var resultAsc = env.executePivot(
                    pivotBuilder
                            .sort(PivotSort.create(numericField, SortSpec.Direction.Ascending))
                            .build());

            expectKeys(resultAsc, "1", "2", "4", "8", "9", "15", "25");
        }
    }

    private void expectKeys(ValidatableResponse response, String... values) {
        for (int i = 0; i < values.length; i++) {
            response.body(".rows[" + i + "].key[1]", is(values[i]));
        }
    }

    private GraylogApis.SearchEnvironment createEnvironment() throws ExecutionException, RetryException {
        return api.createEnvironment(gelfInput);
    }
}
