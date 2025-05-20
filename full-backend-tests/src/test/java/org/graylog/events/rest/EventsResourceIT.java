package org.graylog.events.rest;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = {SearchServer.DATANODE_DEV, SearchServer.OS2_LATEST, SearchServer.ES7})
public class EventsResourceIT {
    private final GraylogApis api;

    public EventsResourceIT(GraylogApis graylogApis) {
        this.api = graylogApis;
    }

    @ContainerMatrixTest
    void testDefaultRequest() {
        given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .post("/events/histogram", Map.of())
                .then()
                .statusCode(HTTP_OK);
    }
}
