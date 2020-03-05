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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.when;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;


@ApiIntegrationTest(serverLifecycle = CLASS)
class BackendStartupIT {

    private final GraylogBackend sut;

    public BackendStartupIT(GraylogBackend sut) {
        this.sut = sut;
    }

    @BeforeAll
    static void beforeAll() {
        RestAssured.requestSpecification =
                new RequestSpecBuilder().build()
                        .accept(JSON)
                        .contentType(JSON)
                        .auth().basic("admin", "admin");
    }

    @Test
    void canReachApi() {
        when()
                .get(sut.apiAddress())
                .then()
                .statusCode(200);
    }

    @Test
    void loadsDefaultPlugins() {
        List<Object> pluginNames = when()
                .get(sut.apiAddress() + "/system/plugins")
                .then()
                .statusCode(200)
                .extract().jsonPath()
                .getList("plugins.name");

        assertThat(pluginNames).containsExactlyInAnyOrder(
                "Threat Intelligence Plugin",
                "Collector",
                "AWS plugins");
    }
}
