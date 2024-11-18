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

import com.google.common.base.Joiner;
import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2_LATEST})
public class SearchAggregationsIT {
    private static final String PIVOT_NAME = "pivotaggregation";
    private static final String PIVOT_PATH = "results.query1.search_types." + PIVOT_NAME;

    private final GraylogApis api;

    public SearchAggregationsIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    public void setUp() {
        this.api.backend().importElasticsearchFixture("random-http-logs.json", SearchAggregationsIT.class);
    }

    @ContainerMatrixTest
    void testZeroPivots() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testZeroPivotsWithLatestMetric() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Latest.builder().field("http_method").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("latest(http_method)")), equalTo("GET"));
    }

    @ContainerMatrixTest
    void testSingleRowPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "count()"), equalTo(860))
                .body(pathToMetricResult("DELETE", "count()"), equalTo(52))
                .body(pathToMetricResult("POST", "count()"), equalTo(45))
                .body(pathToMetricResult("PUT", "count()"), equalTo(43))
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testUnknownFieldsPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().fields(List.of("http_method", "unknown_field_1", "unknown_field_2")).build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("GET", "(Empty Value)", "(Empty Value)"), List.of("count()")), equalTo(860))
                .body(pathToMetricResult(List.of("DELETE", "(Empty Value)", "(Empty Value)"), List.of("count()")), equalTo(52))
                .body(pathToMetricResult(List.of("POST", "(Empty Value)", "(Empty Value)"), List.of("count()")), equalTo(45))
                .body(pathToMetricResult(List.of("PUT", "(Empty Value)", "(Empty Value)"), List.of("count()")), equalTo(43));
    }

    @ContainerMatrixTest
    void testUnknownFieldsAroundUnknownPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().fields(List.of("unknown_field_1", "http_method", "unknown_field_2")).build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("(Empty Value)", "GET", "(Empty Value)"), List.of("count()")), equalTo(860))
                .body(pathToMetricResult(List.of("(Empty Value)", "DELETE", "(Empty Value)"), List.of("count()")), equalTo(52))
                .body(pathToMetricResult(List.of("(Empty Value)", "POST", "(Empty Value)"), List.of("count()")), equalTo(45))
                .body(pathToMetricResult(List.of("(Empty Value)", "PUT", "(Empty Value)"), List.of("count()")), equalTo(43));
    }

    @ContainerMatrixTest
    void testUnknownFieldFirstPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().fields(List.of("unknown_field_1", "http_method")).build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("(Empty Value)", "GET"), List.of("count()")), equalTo(860))
                .body(pathToMetricResult(List.of("(Empty Value)", "DELETE"), List.of("count()")), equalTo(52))
                .body(pathToMetricResult(List.of("(Empty Value)", "POST"), List.of("count()")), equalTo(45))
                .body(pathToMetricResult(List.of("(Empty Value)", "PUT"), List.of("count()")), equalTo(43));
    }


    @ContainerMatrixTest
    void testAllUnknownFieldsPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().fields(List.of("unknown_field_1", "unknown_field_2", "unknown_field_3")).build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("(Empty Value)", "(Empty Value)", "(Empty Value)"), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testFindTopPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().field("http_method").limit(1).build())
                .sort(SeriesSort.create(SeriesSort.Type, "max(took_ms)", SortSpec.Direction.Descending))
                .series(Max.builder().field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "max(took_ms)"), equalTo(5300.0f));
    }

    @ContainerMatrixTest
    void testFindBottomPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().field("http_method").limit(1).build())
                .sort(SeriesSort.create(SeriesSort.Type, "max(took_ms)", SortSpec.Direction.Ascending))
                .series(Max.builder().field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("DELETE", "max(took_ms)"), equalTo(104.0f));
    }

    @ContainerMatrixTest
    void testSingleRowPivotWithDateField() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(
                        Time.builder()
                                .field("timestamp")
                                .interval(TimeUnitInterval.Builder.builder().timeunit("10s").build())
                                .build()
                )
                .series(
                        Count.builder().build(),
                        Average.builder().field("took_ms").build()
                )
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("2022-09-26T14:12:10.000Z", "count()"), equalTo(1))
                .body(pathToMetricResult("2022-09-26T14:12:10.000Z", "avg(took_ms)"), equalTo(51.0f))
                .body(pathToMetricResult("2022-09-26T14:12:20.000Z", "count()"), equalTo(395))
                .body(pathToMetricResult("2022-09-26T14:12:20.000Z", "avg(took_ms)"), equalTo(59.35443037974684f))
                .body(pathToMetricResult("2022-09-26T14:12:30.000Z", "count()"), equalTo(394))
                .body(pathToMetricResult("2022-09-26T14:12:30.000Z", "avg(took_ms)"), equalTo(70.2741116751269f))
                .body(pathToMetricResult("2022-09-26T14:12:40.000Z", "count()"), equalTo(210))
                .body(pathToMetricResult("2022-09-26T14:12:40.000Z", "avg(took_ms)"), equalTo(131.21904761904761f))
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000))
                .body(pathToMetricResult(Collections.emptyList(), List.of("avg(took_ms)")), equalTo(78.74f));
    }

    @ContainerMatrixTest
    void testSingleRowPivotWithDateFieldAsColumnPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .columnGroups(
                        Time.builder()
                                .field("timestamp")
                                .interval(TimeUnitInterval.Builder.builder().timeunit("10s").build())
                                .build()
                )
                .series(Average.builder().field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final List<List<String>> expectedKeys = List.of(List.of("GET"), List.of("DELETE"), List.of("POST"), List.of("PUT"), List.of());

        final String searchTypeResult = PIVOT_PATH + ".rows";

        final List<List<String>> actualRowKeys = validatableResponse.extract().path(searchTypeResult + ".key");

        assertThat(actualRowKeys).isEqualTo(expectedKeys);

        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("GET"), List.of("2022-09-26T14:12:10.000Z", "avg(took_ms)")), equalTo(51.0f))
                .body(pathToMetricResult("DELETE", "avg(took_ms)"), equalTo(73.5576923076923f))
                .body(pathToMetricResult(List.of("DELETE"), List.of("2022-09-26T14:12:10.000Z", "avg(took_ms)")), is(nullValue()))
                .body(pathToMetricResult("GET", "avg(took_ms)"), equalTo(63.14883720930233f));
    }

    @ContainerMatrixTest
    void testSingleColumnPivot() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .columnGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("GET", "count()")), equalTo(860))
                .body(pathToMetricResult(Collections.emptyList(), List.of("DELETE", "count()")), equalTo(52))
                .body(pathToMetricResult(Collections.emptyList(), List.of("POST", "count()")), equalTo(45))
                .body(pathToMetricResult(Collections.emptyList(), List.of("PUT", "count()")), equalTo(43))
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testDoesNotReturnRollupWhenDisabled() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .columnGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("GET", "count()")), equalTo(860))
                .body(pathToMetricResult(Collections.emptyList(), List.of("DELETE", "count()")), equalTo(52))
                .body(pathToMetricResult(Collections.emptyList(), List.of("POST", "count()")), equalTo(45))
                .body(pathToMetricResult(Collections.emptyList(), List.of("PUT", "count()")), equalTo(43))
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), is(nullValue()));
    }

    @ContainerMatrixTest
    void testSingleRowAndColumnPivots() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .columnGroups(Values.builder().field("http_response_code").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse
                .rootPath(searchTypeResultPath + "." + pathToRow(List.of("GET")))
                .body(pathToValue(List.of("200", "count()")), equalTo(847))
                .body(pathToValue(List.of("500", "count()")), equalTo(11))
                .body(pathToValue(List.of("504", "count()")), equalTo(2))
                .body(pathToValue(List.of("count()")), equalTo(860));

        validatableResponse
                .rootPath(searchTypeResultPath + "." + pathToRow(List.of("DELETE")))
                .body(pathToValue(List.of("204", "count()")), equalTo(51))
                .body(pathToValue(List.of("500", "count()")), equalTo(1))
                .body(pathToValue(List.of("count()")), equalTo(52));

        validatableResponse
                .rootPath(searchTypeResultPath + "." + pathToRow(List.of("POST")))
                .body(pathToValue(List.of("201", "count()")), equalTo(43))
                .body(pathToValue(List.of("500", "count()")), equalTo(1))
                .body(pathToValue(List.of("504", "count()")), equalTo(1))
                .body(pathToValue(List.of("count()")), equalTo(45));

        validatableResponse
                .rootPath(searchTypeResultPath + "." + pathToRow(List.of("PUT")))
                .body(pathToValue(List.of("200", "count()")), equalTo(42))
                .body(pathToValue(List.of("504", "count()")), equalTo(1))
                .body(pathToValue(List.of("count()")), equalTo(43));

        validatableResponse
                .rootPath(searchTypeResultPath + "." + pathToRow(Collections.emptySet()))
                .body(pathToValue(List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testRowAndColumnPivotsWithMissingFields() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("missing_row_pivot").build())
                .columnGroups(Values.builder().field("missing_column_pivot").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(2));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("(Empty Value)"), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testTwoNestedRowPivots() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(
                        Values.builder().field("http_method").limit(15).build(),
                        Values.builder().field("http_response_code").limit(15).build()
                )
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(11));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("GET", "200"), List.of("count()")), equalTo(847))
                .body(pathToMetricResult(List.of("GET", "500"), List.of("count()")), equalTo(11))
                .body(pathToMetricResult(List.of("GET", "504"), List.of("count()")), equalTo(2));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("DELETE", "204"), List.of("count()")), equalTo(51))
                .body(pathToMetricResult(List.of("DELETE", "500"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("POST", "201"), List.of("count()")), equalTo(43))
                .body(pathToMetricResult(List.of("POST", "500"), List.of("count()")), equalTo(1))
                .body(pathToMetricResult(List.of("POST", "504"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("PUT", "200"), List.of("count()")), equalTo(42))
                .body(pathToMetricResult(List.of("PUT", "504"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testTwoTupleRowPivots() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(
                        Values.builder().fields(List.of("http_method", "http_response_code")).limit(15).build()
                )
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(11));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("GET", "200"), List.of("count()")), equalTo(847))
                .body(pathToMetricResult(List.of("GET", "500"), List.of("count()")), equalTo(11))
                .body(pathToMetricResult(List.of("GET", "504"), List.of("count()")), equalTo(2));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("DELETE", "204"), List.of("count()")), equalTo(51))
                .body(pathToMetricResult(List.of("DELETE", "500"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("POST", "201"), List.of("count()")), equalTo(43))
                .body(pathToMetricResult(List.of("POST", "500"), List.of("count()")), equalTo(1))
                .body(pathToMetricResult(List.of("POST", "504"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(List.of("PUT", "200"), List.of("count()")), equalTo(42))
                .body(pathToMetricResult(List.of("PUT", "504"), List.of("count()")), equalTo(1));

        validatableResponse
                .rootPath(searchTypeResultPath)
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testTwoNestedRowPivotsWithSorting() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(
                        Values.builder().field("http_method").limit(15).build(),
                        Values.builder().field("http_response_code").limit(15).build()
                )
                .sort(PivotSort.create("http_response_code", SortSpec.Direction.Ascending))
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(10));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        final List<List<String>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.key");
        final List<List<Integer>> metrics = validatableResponse
                .extract()
                .jsonPath()
                .getList(searchTypeResultPath + "*.values*.value");

        assertThat(rows).containsExactly(
                List.of("DELETE", "204"),
                List.of("DELETE", "500"),
                List.of("GET", "200"),
                List.of("GET", "500"),
                List.of("GET", "504"),
                List.of("POST", "201"),
                List.of("POST", "500"),
                List.of("POST", "504"),
                List.of("PUT", "200"),
                List.of("PUT", "504")
        );
        assertThat(metrics).containsExactly(
                List.of(51),
                List.of(1),
                List.of(847),
                List.of(11),
                List.of(2),
                List.of(43),
                List.of(1),
                List.of(1),
                List.of(42),
                List.of(1)
        );
    }

    @ContainerMatrixTest
    void testTwoTupleRowPivotsWithSorting() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(
                        Values.builder().fields(List.of("http_method", "http_response_code")).limit(15).build()
                )
                .sort(PivotSort.create("http_response_code", SortSpec.Direction.Ascending))
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(10));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        final List<List<String>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.key");

        assertThat(rows).containsExactly(
                List.of("GET", "200"),
                List.of("PUT", "200"),
                List.of("POST", "201"),
                List.of("DELETE", "204"),
                List.of("DELETE", "500"),
                List.of("GET", "500"),
                List.of("POST", "500"),
                List.of("GET", "504"),
                List.of("POST", "504"),
                List.of("PUT", "504")
        );
    }

    @ContainerMatrixTest
    void testTwoTupleRowPivotsWithMetricsSorting() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(
                        Values.builder().fields(List.of("action", "controller")).limit(15).build()
                )
                .series(List.of(Max.builder().field("took_ms").build(), Min.builder().field("took_ms").build()))
                .sort(SeriesSort.create("min(took_ms)", SortSpec.Direction.Ascending), SeriesSort.create("max(took_ms)", SortSpec.Direction.Descending))
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(5));

        final List<List<Float>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.value");

        assertThat(rows).containsExactly(
                List.of(5300.0f, 36.0f),
                List.of(5000.0f, 36.0f),
                List.of(174.0f, 36.0f),
                List.of(138.0f, 36.0f),
                List.of(147.0f, 37.0f)
        );
    }

    @ContainerMatrixTest
    void testTwoNestedRowPivotsWithMetricsSorting() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(
                        Values.builder().field("action").limit(15).build(),
                        Values.builder().field("controller").limit(15).build()
                )
                .series(List.of(Max.builder().field("took_ms").build(), Min.builder().field("took_ms").build()))
                .sort(SeriesSort.create("min(took_ms)", SortSpec.Direction.Ascending), SeriesSort.create("max(took_ms)", SortSpec.Direction.Descending))
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(5));

        final List<List<Float>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.value");

        assertThat(rows).containsExactly(
                List.of(5300.0f, 36.0f),
                List.of(147.0f, 37.0f),
                List.of(5000.0f, 36.0f),
                List.of(174.0f, 36.0f),
                List.of(138.0f, 36.0f)
        );
    }

    @ContainerMatrixTest
    void testTopLevelSeries() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .series(List.of(Max.builder().field("took_ms").build(), Min.builder().field("took_ms").build()))
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(1));

        final List<List<Float>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.value");

        assertThat(rows).containsExactly(List.of(5300.0f, 36.0f));
    }

    @ContainerMatrixTest
    void testTwoIdenticalSeries() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(List.of(
                        Max.builder().field("took_ms").build(),
                        Max.builder().field("took_ms").build()
                ))
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(1));

        final List<List<Float>> rows = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.value");

        assertThat(rows).containsExactly(List.of(5300.0f, 5300.0f));
    }

    @ContainerMatrixTest
    void testTwoIdenticalSeriesOneWithCustomId() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(List.of(
                        Max.builder().id("Maximum Response Time").field("took_ms").build(),
                        Max.builder().field("took_ms").build()
                ))
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000));

        final String searchTypeResultPath = PIVOT_PATH + ".rows";

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
                .body("rows", hasSize(1));

        final List<List<List<String>>> rowKeys = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.key");

        assertThat(rowKeys).containsExactly(List.of(
                Collections.singletonList("Maximum Response Time"),
                Collections.singletonList("max(took_ms)")
        ));

        final List<List<Float>> rowValues = validatableResponse
                .extract()
                .jsonPath().getList(searchTypeResultPath + "*.values*.value");

        assertThat(rowValues).containsExactly(List.of(5300.0f, 5300.0f));
    }

    // Percentage Metric tests
    @ContainerMatrixTest
    void testSimplestPercentageMetricWithCount() {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().field("http_method").build())
                .series(Percentage.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "percentage(,COUNT)"), equalTo(0.86f))
                .body(pathToMetricResult("DELETE", "percentage(,COUNT)"), equalTo(0.052f))
                .body(pathToMetricResult("POST", "percentage(,COUNT)"), equalTo(0.045f))
                .body(pathToMetricResult("PUT", "percentage(,COUNT)"), equalTo(0.043f));
    }

    @ContainerMatrixTest
    void testPercentageMetricWithCountOnField() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .series(Percentage.builder().strategy(Percentage.Strategy.COUNT).field("http_method").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "percentage(http_method,COUNT)"), equalTo(0.86f))
                .body(pathToMetricResult("DELETE", "percentage(http_method,COUNT)"), equalTo(0.052f))
                .body(pathToMetricResult("POST", "percentage(http_method,COUNT)"), equalTo(0.045f))
                .body(pathToMetricResult("PUT", "percentage(http_method,COUNT)"), equalTo(0.043f));
    }

    @ContainerMatrixTest
    void testPercentageMetricWithCountOnFieldForColumnPivotOnly() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .columnGroups(Values.builder().field("http_method").build())
                .series(Percentage.builder().strategy(Percentage.Strategy.COUNT).field("http_method").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of(), List.of("GET", "percentage(http_method,COUNT)")), equalTo(0.86f))
                .body(pathToMetricResult(List.of(), List.of("DELETE", "percentage(http_method,COUNT)")), equalTo(0.052f))
                .body(pathToMetricResult(List.of(), List.of("POST", "percentage(http_method,COUNT)")), equalTo(0.045f))
                .body(pathToMetricResult(List.of(), List.of("PUT", "percentage(http_method,COUNT)")), equalTo(0.043f));
    }

    @ContainerMatrixTest
    void testPercentageMetricWithSumOnField() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .series(Percentage.builder().strategy(Percentage.Strategy.SUM).field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(5));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "percentage(took_ms,SUM)"), equalTo(0.689713f))
                .body(pathToMetricResult("DELETE", "percentage(took_ms,SUM)"), equalTo(0.04857759715519431f))
                .body(pathToMetricResult("POST", "percentage(took_ms,SUM)"), equalTo(0.148501397002794f))
                .body(pathToMetricResult("PUT", "percentage(took_ms,SUM)"), equalTo(0.11320802641605283f));
    }

    @ContainerMatrixTest
    void testBooleanFieldsAreReturnedAsTrueOrFalse() {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("test_boolean").build(), Values.builder().field("user_id").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = executePivot(pivot);

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(List.of("true", "6476752"), List.of("count()")), equalTo(1))
                .body(pathToMetricResult(List.of("false", "6469981"), List.of("count()")), equalTo(1));
    }

    private String listToGroovy(Collection<String> strings) {
        final List<String> quotedStrings = strings.stream()
                .map(string -> "'" + string + "'")
                .collect(Collectors.toList());

        final String quotedList = Joiner.on(", ").join(quotedStrings);
        return "[" + quotedList + "]";
    }

    private String pathToMetricResult(String key, String metric) {
        return pathToMetricResult(List.of(key), List.of(metric));
    }

    private String pathToValue(Collection<String> metric) {
        return "values.find { value -> value.key == " + listToGroovy(metric) + " }.value";
    }

    private String pathToRow(Collection<String> keys) {
        return "find { it.key == " + listToGroovy(keys) + " }";
    }

    private String pathToMetricResult(Collection<String> keys, Collection<String> metric) {
        return pathToRow(keys) + "." + pathToValue(metric);
    }

    private ValidatableResponse executePivot(Pivot pivot) {
        return api.search().executePivot(pivot)
                .body(".total", equalTo(1000));
    }
}
