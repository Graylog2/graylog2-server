package org.graylog.testing.completebackend.apis;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class Dashboards implements GraylogRestApi {
    private final GraylogApis api;

    public Dashboards(GraylogApis api) {
        this.api = api;
    }

    public String createDashboard(String title) {
        final var searchId = given()
                .spec(api.requestSpecification())
                .when()
                .body(Map.of())
                .post("/views/search")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
        return given()
                .spec(api.requestSpecification())
                .when()
                .body(createDashboardRequest(title, searchId))
                .post("/views")
                .then()
                .log().ifError()
                .statusCode(201)
                .assertThat().body("id", notNullValue())
                .extract().body().jsonPath().getString("id");
    }

    private Map<String, Object> createDashboardRequest(String title, String searchId) {
        return Map.of(
                "title", title,
                "search_id", searchId,
                "state", Map.of(),
                "type", "DASHBOARD"
        );
    }
}
