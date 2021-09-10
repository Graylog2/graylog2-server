package org.graylog.plugins.views;

import io.restassured.specification.RequestSpecification;
import org.graylog.storage.elasticsearch7.ElasticsearchInstanceES7Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.core.IsEqual.equalTo;

@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES7Factory.class)
public class SearchSyncIT {

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SearchSyncIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/search/sync")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("Search body is mandatory"));
    }

    @Test
    void testMinimalisticRequest() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/minimalistic-request.json"))
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .assertThat().body("execution.completed_exceptionally", equalTo(false));
    }
}
