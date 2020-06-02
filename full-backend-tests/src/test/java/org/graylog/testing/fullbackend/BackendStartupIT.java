/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.testing.fullbackend;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.backenddriver.SearchDriver.searchAllMessages;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;


@ApiIntegrationTest(serverLifecycle = CLASS)
class BackendStartupIT {

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public BackendStartupIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void canReachApi() {
        given()
                .spec(requestSpec)
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    void loadsDefaultPlugins() {
        List<Object> pluginNames =
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/system/plugins")
                        .then()
                        .statusCode(200)
                        .extract().jsonPath()
                        .getList("plugins.name");

        assertThat(pluginNames).containsExactlyInAnyOrder(
                "Threat Intelligence Plugin",
                "Collector",
                "AWS plugins",
                "Elasticsearch 6 Support");
    }

    @Test
    void importsElasticsearchFixtures() {
        sut.importElasticsearchFixture("one-message.json", getClass());

        List<String> allMessages = searchAllMessages(requestSpec);

        assertThat(allMessages).containsExactly("hello from es fixture");
    }
}
