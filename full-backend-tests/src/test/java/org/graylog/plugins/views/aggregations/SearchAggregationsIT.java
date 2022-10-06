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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2;
import static org.graylog.testing.containermatrix.SearchServer.OS2_2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, searchVersions = {ES7, OS1, OS2, OS2_2})
public class SearchAggregationsIT {
    private static final String PIVOT_NAME = "pivotaggregation";
    private static final String PIVOT_PATH = "results.query1.search_types." + PIVOT_NAME;

    private final RequestSpecification requestSpec;
    private final GraylogBackend backend;

    public SearchAggregationsIT(RequestSpecification requestSpec, GraylogBackend backend) {
        this.requestSpec = requestSpec;
        this.backend = backend;
    }

    @BeforeAll
    public void setUp() {
        backend.importElasticsearchFixture("random-http-logs.json", SearchAggregationsIT.class);
    }

    private ValidatableResponse execute(Pivot pivot) throws JsonProcessingException {
        final Pivot pivotWithId = pivot.toBuilder()
                .id(PIVOT_NAME)
                .build();

        final SearchDTO search = SearchDTO.builder()
                .queries(QueryDTO.builder()
                        .timerange(RelativeRange.create(0))
                        .id("query1")
                        .query(ElasticsearchQueryString.of("source:pivot-fixtures"))
                        .searchTypes(Set.of(pivotWithId))
                        .build())
                .build();

        return given()
                .spec(requestSpec)
                .when()
                .body(serialize(search))
                .post("/views/search/sync")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .body("execution.done", equalTo(true))
                .body("execution.completed_exceptionally", equalTo(false))
                .body(PIVOT_PATH + ".total", equalTo(1000));
    }

    @ContainerMatrixTest
    void testZeroPivots() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("count()")), equalTo(1000));
    }

    @ContainerMatrixTest
    void testZeroPivotsWithLatestMetric() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .series(Latest.builder().field("http_method").build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult(Collections.emptyList(), List.of("latest(http_method)")), equalTo("GET"));
    }

    @ContainerMatrixTest
    void testSingleRowPivot() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "count()"), equalTo(860))
                .body(pathToMetricResult("DELETE", "count()"), equalTo(52))
                .body(pathToMetricResult("POST", "count()"), equalTo(45))
                .body(pathToMetricResult("PUT", "count()"), equalTo(43));
    }

    @ContainerMatrixTest
    void testFindTopPivot() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().field("http_method").limit(1).build())
                .sort(SeriesSort.create(SeriesSort.Type, "max(took_ms)", SortSpec.Direction.Descending))
                .series(Max.builder().field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("GET", "max(took_ms)"), equalTo(5300.0f));
    }

    @ContainerMatrixTest
    void testFindBottomPivot() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .rowGroups(Values.builder().field("http_method").limit(1).build())
                .sort(SeriesSort.create(SeriesSort.Type, "max(took_ms)", SortSpec.Direction.Ascending))
                .series(Max.builder().field("took_ms").build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(1));

        final String searchTypeResult = PIVOT_PATH + ".rows";
        validatableResponse
                .rootPath(searchTypeResult)
                .body(pathToMetricResult("DELETE", "max(took_ms)"), equalTo(104.0f));
    }

    @ContainerMatrixTest
    void testSingleRowPivotWithDateField() throws JsonProcessingException {
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

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

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
                .body(pathToMetricResult("2022-09-26T14:12:40.000Z", "avg(took_ms)"), equalTo(131.21904761904761f));
    }

    @ContainerMatrixTest
    void testSingleRowPivotWithDateFieldAsColumnPivot() throws JsonProcessingException {
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

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(4));

        final List<List<String>> expectedKeys = List.of(List.of("GET"), List.of("DELETE"), List.of("POST"), List.of("PUT"));

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
    void testSingleColumnPivot() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .columnGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

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
    void testDoesNotReturnRollupWhenDisabled() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(false)
                .columnGroups(Values.builder().field("http_method").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

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
    void testSingleRowAndColumnPivots() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(Values.builder().field("http_method").build())
                .columnGroups(Values.builder().field("http_response_code").build())
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

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
    void testTwoRowPivots() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(
                        Values.builder().field("http_method").limit(15).build(),
                        Values.builder().field("http_response_code").limit(15).build()
                )
                .series(Count.builder().build())
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("rows", hasSize(10));

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

    private InputStream serialize(Object request) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(request));
    }
}
