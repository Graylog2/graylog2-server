package org.graylog2.suggestions;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration
public class EntitySuggestionsIT {
    private final GraylogApis api;

    public EntitySuggestionsIT(GraylogApis api) {
        this.api = api;
    }

    @Test
    void returnsTitlesForDashboards() {
        final var dashboard1 = api.dashboards().createDashboard("Test");
        final var dashboard2 = api.dashboards().createDashboard("Test 2");
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .get("/entity_suggestions?page=1&per_page=100&collection=dashboards&column=title")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("pagination.total", equalTo(2))
                .body("suggestions[0].value", equalTo("Test"))
                .body("suggestions[0].id", equalTo(dashboard1))
                .body("suggestions[1].value", equalTo("Test 2"))
                .body("suggestions[1].id", equalTo(dashboard2));
    }
}
