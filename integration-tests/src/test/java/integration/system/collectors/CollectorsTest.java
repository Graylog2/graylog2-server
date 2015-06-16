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
package integration.system.collectors;

import integration.BaseRestTest;
import integration.RequiresVersion;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.jodatime.api.Assertions.assertThat;

@RequiresVersion(">=1.1.0")
public class CollectorsTest extends BaseRestTest {
    private final String resourcePrefix = "/system/collectors";

    @Test
    public void testRegisterCollector() throws Exception {
        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResourceForMethod())
                    .put(getResourceEndpoint("collectorId"))
                .then()
                    .statusCode(202);
    }

    @Test
    public void testRegisterInvalidCollector() throws Exception {
        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResourceForMethod())
                    .put(getResourceEndpoint("invalidCollector"))
                .then()
                    .statusCode(400);
    }

    @Test
    public void testListCollectors() throws Exception {
        given().when()
                    .get(resourcePrefix)
                .then()
                    .statusCode(200)
                    .assertThat().body("collectors", notNullValue());
    }

    @Test
    public void testGetCollector() throws Exception {
        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResourceForMethod())
                    .put(getResourceEndpoint("getCollectorTest"))
                .then()
                    .statusCode(202);

        given().when()
                    .get(resourcePrefix + "/getCollectorTest")
                .then()
                    .statusCode(200)
                    .assertThat()
                        .body("id", is("getCollectorTest"))
                        .body(".", containsAllKeys("id", "node_id", "node_details", "last_seen", "active"))
                        .body("active", is(true));
    }

    @Test
    public void testTouchCollector() throws Exception {
        final String collectorId = "testTouchCollectorId";

        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResourceForMethod())
                    .put(getResourceEndpoint(collectorId))
                .then()
                    .statusCode(202);

        final DateTime lastSeenBefore = getLastSeenForCollectorId(collectorId);

        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResource("test-register-collector.json"))
                    .put(getResourceEndpoint(collectorId))
                .then()
                    .statusCode(202);

        final DateTime lastSeenAfterOtherRegistration = getLastSeenForCollectorId(collectorId);

        given().when()
                    .header("X-Graylog-Collector-Version", "0.0.0")
                    .body(jsonResourceForMethod())
                    .put(getResourceEndpoint(collectorId))
                .then()
                    .statusCode(202);

        final DateTime lastSeenAfter = getLastSeenForCollectorId(collectorId);

        assertThat(lastSeenBefore).isEqualTo(lastSeenAfterOtherRegistration);
        assertThat(lastSeenBefore)
                .isNotEqualTo(lastSeenAfter)
                .isBefore(lastSeenAfter);
    }

    private DateTime getLastSeenForCollectorId(String collectorId) {
        final String content = get(resourcePrefix).asString();
        final List<String> lastSeenStringsBefore = from(content).get("collectors.findAll { collector -> collector.id == \"" + collectorId + "\" }.last_seen");
        assertThat(lastSeenStringsBefore).isNotEmpty().hasSize(1);

        return DateTime.parse(lastSeenStringsBefore.get(0));
    }

    private String getResourceEndpoint(String collectorId) {
        return resourcePrefix + "/" + collectorId;
    }
}
