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
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;
import static org.hamcrest.Matchers.is;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2_LATEST})
public class AggregationSortingIT {
    private static final String numericField = "numeric_field";
    private static final String nonNumericField = "non_numeric_field";

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
        final var values = Set.of(9, 8, 4, 25, 2, 15, 1);
        final var messagePrefix = "sorting on numeric pivot test ";
        try (final var env = createEnvironment()) {
            IntStream.range(0, 3).forEach((i) -> {
                for (final var value : values) {
                    env.ingestMessage(Map.of(
                            nonNumericField, "foo",
                            numericField, value,
                            "short_message", messagePrefix + value
                    ));
                }
            });

            final var pivotBuilder = Pivot.builder()
                    .rowGroups(Values.builder()
                            .fields(List.of(nonNumericField, numericField)).limit(10).build())
                    .series(List.of())
                    .rollup(false);

            env.waitForMessages(values.stream().map(value -> messagePrefix + value).toList());

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

    @ContainerMatrixTest
    void sortingOnNonNumericPivotFieldSortsLexicographically() throws ExecutionException, RetryException {
        final var values = Set.of("B", "C", "D", "A", "E");
        final var messagePrefix = "sorting on non-numeric pivot test ";
        try (final var env = createEnvironment()) {
            IntStream.range(0, 3).forEach((i) -> {
                for (final var value : values) {
                    env.ingestMessage(Map.of(
                            nonNumericField, value,
                            numericField, 42,
                            "short_message", messagePrefix + value
                    ));
                }
            });

            final var pivotBuilder = Pivot.builder()
                    .rowGroups(Values.builder()
                            .fields(List.of(numericField, nonNumericField)).limit(10).build())
                    .series(List.of())
                    .rollup(false);

            env.waitForMessages(values.stream().map(value -> messagePrefix + value).toList());

            env.waitForFieldTypes(numericField);

            final var resultDesc = env.executePivot(
                    pivotBuilder
                            .sort(PivotSort.create(nonNumericField, SortSpec.Direction.Ascending))
                            .build()
            );
            assertThat(resultDesc).isNotNull();

            expectKeys(resultDesc, "A", "B", "C", "D", "E");

            final var resultAsc = env.executePivot(
                    pivotBuilder
                            .sort(PivotSort.create(nonNumericField, SortSpec.Direction.Descending))
                            .build());

            expectKeys(resultAsc, "E", "D", "C", "B", "A");
        }
    }

    @ContainerMatrixTest
    void sortingOnBothNumericFieldAndMetric() throws ExecutionException, RetryException {
        final var values = List.of(2, 4, 9, 1, 25, 2, 9, 4, 15);
        final var messagePrefix = "Ingesting value ";
        try (final var env = createEnvironment()) {
            IntStream.range(0, 3).forEach((i) -> {
                for (final var value : values) {
                    env.ingestMessage(Map.of(
                            nonNumericField, "Test",
                            numericField, value,
                            "short_message", messagePrefix + value
                    ));
                }
            });

            env.waitForMessages(values.stream().distinct().map(value -> messagePrefix + value).toList());

            env.waitForFieldTypes(numericField);

            final var pivotBuilder = Pivot.builder()
                    .rowGroups(Values.builder()
                            .fields(List.of(nonNumericField, numericField)).limit(10).build())
                    .series(List.of(Count.builder().build()))
                    .rollup(false);

            final var resultAsc = env.executePivot(
                    pivotBuilder
                            .sort(
                                    PivotSort.create(numericField, SortSpec.Direction.Ascending),
                                    SeriesSort.create("count()", SortSpec.Direction.Descending)
                            )
                            .build()
            );

            expectKeys(resultAsc, "1", "2", "4", "9", "15", "25");

            final var resultDesc = env.executePivot(
                    pivotBuilder
                            .sort(
                                    PivotSort.create(numericField, SortSpec.Direction.Descending),
                                    SeriesSort.create("count()", SortSpec.Direction.Descending)
                            )
                            .build()
            );

            expectKeys(resultDesc, "25", "15", "9", "4", "2", "1");
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
