package org.graylog.testing.utils;

import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;

public final class GelfInputUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GelfInputUtils.class);

    private GelfInputUtils() { }

    public static void createGelfHttpInput(int mappedPort, int gelfHttpPort, RequestSpecification requestSpecification) {
        InputCreateRequest request = InputCreateRequest.create(
                "KILL ME",
                GELFHttpInput.class.getName(),
                true,
                ImmutableMap.of("bind_address", "0.0.0.0", "port", gelfHttpPort),
                null);

        given()
                .spec(requestSpecification)
                .body(request)
                .expect().response().statusCode(201)
                .when()
                .post("/system/inputs");

        waitForGelfInputOnPort(mappedPort, requestSpecification);
    }

    private static void waitForGelfInputOnPort(int mappedPort, RequestSpecification requestSpecification) {
        WaitUtils.waitFor(
                () -> gelfInputIsListening(mappedPort, requestSpecification),
                "Timed out waiting for GELF input listening on port " + mappedPort);
    }

    private static boolean gelfInputIsListening(int mappedPort, RequestSpecification requestSpecification) {
        try {
            gelfEndpoint(mappedPort, requestSpecification)
                    .expect().response().statusCode(200)
                    .when()
                    .options();
            LOG.info("GELF input listening on port {}", mappedPort);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static RequestSpecification gelfEndpoint(int mappedPort, RequestSpecification requestSpecification) {
        return given()
                .spec(requestSpecification)
                .basePath("/gelf")
                .port(mappedPort);
    }

    public static void postMessage(int mappedPort,
                                   @SuppressWarnings("SameParameterValue") String messageJson,
                                   RequestSpecification requestSpecification) {
        gelfEndpoint(mappedPort, requestSpecification)
                .body(messageJson)
                .expect().response().statusCode(202)
                .when()
                .post();
    }
}
