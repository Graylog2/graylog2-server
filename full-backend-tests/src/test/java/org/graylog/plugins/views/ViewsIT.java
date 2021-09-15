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
public class ViewsIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public ViewsIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void testEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/")
                .then()
                .statusCode(400)
                .assertThat().body("message[0]", equalTo("View is mandatory"));
    }

    @Test
    void testCreateViewRequestWithoutPersistedSearch() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/views-request.json"))
                .post("/views")
                .then()
                .statusCode(400)
                .assertThat().body("message", equalTo("Search 6141d457d3a6b9d73c8ac55a doesn't exist"));
    }

    @Test
    void testCreateSearchPersistView() {
        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/save-search-request.json"))
                .post("/views/search")
                .then()
                .statusCode(201);

        given()
                .spec(requestSpec)
                .when()
                .body(getClass().getClassLoader().getResourceAsStream("org/graylog/plugins/views/views-request.json"))
                .post("/views")
                .then()
                .statusCode(200);
    }
}
