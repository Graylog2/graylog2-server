package org.graylog2.streams;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.IndexSetUtils;
import org.graylog.testing.utils.StreamUtils;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(mongoVersions = MongodbServer.MONGO5)
public class StreamsIT {
    private static final String STREAMS_RESOURCE = "/streams";

    private final RequestSpecification requestSpec;

    private String defaultIndexSetId;
    private String newIndexSetId;
    private String newIndexSetId2;
    private String stream1Id;
    private String stream2Id;
    private String stream3Id;

    public StreamsIT(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    void beforeAll() {
        this.defaultIndexSetId = IndexSetUtils.defaultIndexSetId(requestSpec);
        this.newIndexSetId = IndexSetUtils.createIndexSet(requestSpec, "Test Indices", "Some test indices", "test");
        this.newIndexSetId2 = IndexSetUtils.createIndexSet(requestSpec, "More Test Indices", "Some more test indices", "moretest");
        this.stream1Id = StreamUtils.createStream(requestSpec, "New Stream", newIndexSetId);
        this.stream2Id = StreamUtils.createStream(requestSpec, "New Stream 2", defaultIndexSetId);
        this.stream3Id = StreamUtils.createStream(requestSpec, "New Stream 3", newIndexSetId2);
    }

    @ContainerMatrixTest
    void sortByIndexSetTitle() {
        paginatedByFieldWithOrder("title", "asc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream", "New Stream 2", "New Stream 3")));
        paginatedByFieldWithOrder("title", "desc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream 3", "New Stream 2", "New Stream")));
        paginatedByFieldWithOrder("index_set_title", "asc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream 2", "New Stream 3", "New Stream")));
        paginatedByFieldWithOrder("index_set_title", "desc")
                .assertThat()
                .body("streams*.title", equalTo(List.of("New Stream", "New Stream 3", "New Stream 2")));
    }

    private ValidatableResponse paginatedByFieldWithOrder(String field, String order) {
        return given()
                .spec(requestSpec)
                .log().ifValidationFails()
                .when()
                .queryParam("query", "New")
                .queryParam("sort", field)
                .queryParam("order", order)
                .get(STREAMS_RESOURCE + "/paginated")
                .then()
                .log().ifValidationFails();
    }
}
