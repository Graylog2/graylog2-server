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
package integration.alarmcallbacks;

import com.jayway.restassured.path.json.JsonPath;
import integration.BaseRestTest;
import integration.MongoDbSeed;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("legacy test that should be converted or deleted")
@RequiresAuthentication
@RequiresVersion(">1.1.99")
@MongoDbSeed
public class AlarmCallbackHistoryResourceTest extends BaseRestTest {
    private static final String alertId = "55756c223b0c1b78809e911d";
    private static final String streamId = "552b92b2e4b0c055e41ffb8e";
    private static final String url = buildUrl(streamId, alertId);

    @Test
    public void listHistoriesWhenNoHistoriesArePresent() throws Exception {
        final JsonPath response = given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .assertThat()
                .body(".", containsAllKeys("total", "histories"))
                .extract().jsonPath();

        assertThat(response.getInt("total")).isEqualTo(0);
        assertThat(response.getList("histories")).isEmpty();
    }

    @Test
    public void getHistoryForAlert() throws Exception {
        final JsonPath response = given()
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .assertThat()
                .body(".", containsAllKeys("total", "histories"))
                .extract().jsonPath();

        assertThat(response.getInt("total")).isEqualTo(2);
        assertThat(response.getList("histories")).isNotEmpty();
    }

    @Test
    public void listHistoriesForNonExistingStreamShouldFail() throws Exception {
        given()
                .when()
                .get(buildUrl("nonexisting", alertId))
                .then()
                .statusCode(404);
    }

    @Test
    public void listHistoriesForNonExistingAlertShouldFail() throws Exception {
        given()
                .when()
                .get(buildUrl(streamId, "nonexisting"))
                .then()
                .statusCode(404);
    }

    private static String buildUrl(String streamId, String alertId) {
        return "/streams/" + streamId + "/alerts/" + alertId + "/history";
    }
}
