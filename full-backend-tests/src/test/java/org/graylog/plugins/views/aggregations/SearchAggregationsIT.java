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
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2;
import static org.graylog.testing.containermatrix.SearchServer.OS2_2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5, searchVersions = {ES7, OS1, OS2, OS2_2})
public class SearchAggregationsIT {
    private static final String PIVOT_NAME = "pivot-aggregation";
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
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .body("execution.done", equalTo(true))
                .body(PIVOT_PATH + ".total", equalTo(1000));
    }

    @ContainerMatrixTest
    void testSingleRowPivot() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(List.of(Values.builder().field("http_method").build()))
                .series(List.of(Count.builder().build()))
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

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
    void testSingleRowAndColumnPivots() throws JsonProcessingException {
        final Pivot pivot = Pivot.builder()
                .rollup(true)
                .rowGroups(List.of(Values.builder().field("http_method").build()))
                .columnGroups(List.of(Values.builder().field("http_response_code").build()))
                .series(List.of(Count.builder().build()))
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
                .rowGroups(List.of(
                        Values.builder().field("http_method").build(),
                        Values.builder().field("http_response_code").build()
                ))
                .series(List.of(Count.builder().build()))
                .build();

        final ValidatableResponse validatableResponse = execute(pivot);

        validatableResponse.rootPath(PIVOT_PATH)
                .body("total", equalTo(1000))
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
                .map(string -> "\"" + string + "\"")
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
        return "find { row -> row.key == " + listToGroovy(keys) + " }";
    }

    private String pathToMetricResult(Collection<String> keys, Collection<String> metric) {
        return pathToRow(keys) + "." + pathToValue(metric);
    }

    private InputStream serialize(Object request) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(request));
    }
}
