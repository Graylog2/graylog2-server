package org.graylog.testing.utils;

import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class IndexSetUtils {
    private IndexSetUtils() {}

    public static String defaultIndexSetId(RequestSpecification requestSpec) {
        return given()
                .spec(requestSpec)
                .when()
                .get("/system/indices/index_sets")
                .then()
                .statusCode(200)
                .assertThat()
                .extract().body().jsonPath().getString("index_sets.find { it.default == true }.id");
    }
}
