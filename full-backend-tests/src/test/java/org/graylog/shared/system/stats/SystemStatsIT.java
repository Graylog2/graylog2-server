package org.graylog.shared.system.stats;

import io.restassured.specification.RequestSpecification;
import org.graylog.storage.elasticsearch6.ElasticsearchInstanceES6Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES6Factory.class)
public class SystemStatsIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public SystemStatsIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void filesystemStats() {
        final Map<Object, Object> filesystems = given()
                .spec(requestSpec)
                .when()
                .get("/system/stats")
                .then()
                .statusCode(200)
                .extract().jsonPath().getMap("fs.filesystems");

        assertThat(filesystems).isNotEmpty();
        assertThat(filesystems.get("/usr/share/graylog/data/journal")).satisfies(entry ->
                assertThat(((HashMap) entry).get("mount")).isEqualTo("/"));
    }
}
