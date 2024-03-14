package org.graylog.events;

import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.rest.SearchDTO;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeAll;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;
import static org.graylog.testing.utils.SerializationUtils.serialize;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2_LATEST})
public class EventsSearchTypeIT {
    private static final String PIVOT_PATH_PREFIX = "results.query1.search_types.";

    private final GraylogApis api;

    public EventsSearchTypeIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    public void setUp() {
        this.api.backend().importElasticsearchFixture("basic-events.json", EventsSearchTypeIT.class);
    }

    private ValidatableResponse executeSearch(SearchDTO search) {
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(serialize(search))
                .post("/views/search/sync")
                .then()
                .log().ifError()
                .log().ifValidationFails()
                .statusCode(200)
                .body("execution.done", equalTo(true))
                .body("execution.completed_exceptionally", equalTo(false));
    }

    private ValidatableResponse executeEventsSearchType(EventList eventList) {
        final SearchDTO search = SearchDTO.builder()
                .queries(QueryDTO.builder()
                        .timerange(RelativeRange.create(0))
                        .id("query1")
                        .query(ElasticsearchQueryString.of("source:pivot-fixtures"))
                        .searchTypes(Set.of(eventList))
                        .build())
                .build();
        return executeSearch(search)
                .rootPath(PIVOT_PATH_PREFIX + eventList.id());
    }

    @ContainerMatrixTest
    void testPlainEventsListReturnsAllEvents() {
        final var eventList = EventList.builder()
                .build();
        executeEventsSearchType(eventList);
    }
}
