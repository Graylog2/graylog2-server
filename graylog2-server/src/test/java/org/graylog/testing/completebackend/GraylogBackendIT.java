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
package org.graylog.testing.completebackend;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;


@ApiIntegrationTest
class GraylogBackendIT {

    private final GraylogBackend sut;

    public GraylogBackendIT(GraylogBackend sut) {
        this.sut = sut;
    }

    @BeforeAll
    static void beforeAll() {
        RestAssured.requestSpecification =
                new RequestSpecBuilder().build()
                        .accept(JSON)
                        .contentType(JSON)
                        .authentication().preemptive().basic("admin", "admin");
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
