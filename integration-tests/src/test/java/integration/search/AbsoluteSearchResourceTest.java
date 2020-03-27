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
package integration.search;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import integration.RequiresAuthentication;
import integration.RestTestIncludingElasticsearch;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.junit.Ignore;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@RequiresAuthentication
@Ignore("Flakey test")
public class AbsoluteSearchResourceTest extends RestTestIncludingElasticsearch {
    @Test
    @UsingDataSet(locations = "searchForExistingKeyword.json")
    public void searchForAllMessages() {
        final ValidatableResponse result = doSearchFor("*");
        final JsonPath response = result
                .statusCode(200)
                .extract().jsonPath();

        assertThat(response.getInt("total_results")).isEqualTo(2);
        assertThat(response.getList("messages")).hasSize(2);
        assertThat(response.getList("used_indices")).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "searchForExistingKeyword.json")
    public void searchForExistingKeyword() {
        final ValidatableResponse result = doSearchFor("Testmessage");
        final JsonPath response = result
                .statusCode(200)
                .extract().jsonPath();

        assertThat(response.getInt("total_results")).isEqualTo(1);
        assertThat(response.getList("messages")).hasSize(1);
        assertThat(response.getList("used_indices")).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "searchForExistingKeyword.json")
    public void searchForNonexistingKeyword() {
        final ValidatableResponse result = doSearchFor("Nonexistent");
        final JsonPath response = result
                .statusCode(200)
                .extract().jsonPath();

        assertThat(response.getInt("total_results")).isEqualTo(0);
        assertThat(response.getList("messages")).isEmpty();
        assertThat(response.getList("used_indices")).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "searchForExistingKeyword.json")
    public void searchForExistingKeywordOutsideOfTimeRange() {
        final ValidatableResponse result = doSearchFor("Testmessage", "2015-06-14T11:32:16.827Z", "2015-06-15T11:32:16.827Z");
        final JsonPath response = result
                .statusCode(200)
                .extract().jsonPath();

        assertThat(response.getInt("total_results")).isEqualTo(0);
        assertThat(response.getList("messages")).isEmpty();
        assertThat(response.getList("used_indices")).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "searchForExistingKeyword.json")
    public void searchForExistingKeywordInsideOfVeryNarrowTimeRange() {
        final ValidatableResponse result = doSearchFor("Testmessage", "2015-06-16T11:32:16.827Z", "2015-06-16T11:32:16.827Z");
        final JsonPath response = result
                .statusCode(200)
                .extract().jsonPath();

        assertThat(response.getInt("total_results")).isEqualTo(1);
        assertThat(response.getList("messages")).isNotEmpty();
        assertThat(response.getList("used_indices")).hasSize(1);
    }

    protected ValidatableResponse doSearchFor(String query) {
        return doSearchFor(query, "1971-03-14T15:09:26.540Z", "2015-08-14T15:09:26.540Z");
    }

    protected ValidatableResponse doSearchFor(String query, String from, String to) {
        final ValidatableResponse result = given()
                .when()
                .param("query", query)
                .param("from", from)
                .param("to", to)
                .get("/search/universal/absolute")
                .then()
                .body(".", containsAllKeys("query", "built_query", "used_indices", "messages", "fields", "time", "total_results", "from", "to"));

        final JsonPath resultBody = result.extract().jsonPath();

        assertThat(resultBody.getString("query")).isEqualTo(query);

        return result;
    }
}
