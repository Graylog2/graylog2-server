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
package org.graylog.plugins.views;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@ApiIntegrationTest(serverLifecycle = CLASS)
class MessagesResourceIT {
    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public MessagesResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void canDownloadCsv() {
        sut.importElasticsearchFixture("messages-for-export.json", getClass());

        String allMessagesTimeRange = "{\"timerange\": {\"type\": \"absolute\", \"from\": \"2015-01-01T00:00:00\", \"to\": \"2015-01-01T23:59:59\"}}";

        Response r = given()
                .spec(requestSpec)
                .accept("text/csv")
                .body(allMessagesTimeRange)
                .expect().response().statusCode(200).contentType("text/csv")
                .when()
                .post("/views/search/messages");

        assertThat(r.asString().split("\n")).containsExactly(
                "\"timestamp\",\"source\",\"message\"",
                "\"2015-01-01 04:00:00.000\",\"source-2\",\"Ho\"",
                "\"2015-01-01 03:00:00.000\",\"source-1\",\"Hi\"",
                "\"2015-01-01 02:00:00.000\",\"source-2\",\"He\"",
                "\"2015-01-01 01:00:00.000\",\"source-1\",\"Ha\""
        );
    }
}
