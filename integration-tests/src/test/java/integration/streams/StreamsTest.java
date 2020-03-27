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
package integration.streams;

import integration.BaseRestTest;
import integration.MongoDbSeed;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.junit.Ignore;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Ignore("legacy test that should be converted or deleted")
@RequiresAuthentication
@RequiresVersion(">=0.90.0")
@MongoDbSeed
public class StreamsTest extends BaseRestTest {
    @Test
    public void listStreamsWhenNoStreamsArePresent() throws Exception {
        final JsonPath response = given()
                .when()
                .get("/streams")
                .then()
                .statusCode(200)
                .assertThat()
                .body(".", containsAllKeys("total", "streams"))
                .extract().jsonPath();

        assertThat(response.getInt("total")).isEqualTo(0);
        assertThat(response.getList("streams")).isEmpty();
    }

    @Test
    public void createStreamByTitleOnly() throws Exception {
        final int beforeCount = streamCount();

        final JsonPath response = createStreamFromRequest(jsonResourceForMethod())
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount+1);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("title", equalTo("TestStream"))
                .body("disabled", equalTo(true))
                .body("matching_type", equalTo("AND"))
                .body("description", equalTo(null));
    }

    @Test
    public void createStreamWithTitleAndDescription() throws Exception {
        final int beforeCount = streamCount();
        final String streamTitle = "Another Test Stream";
        final String description = "This is a test stream.";

        final JsonPath response = createStreamFromRequest(jsonResourceForMethod())
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount+1);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("title", equalTo(streamTitle))
                .body("disabled", equalTo(true))
                .body("matching_type", equalTo("AND"))
                .body("description", equalTo(description));
    }

    @Test
    public void createOrMatchingStreamWithTitleAndDescription() throws Exception {
        final int beforeCount = streamCount();
        final String streamTitle = "Another Test Stream";
        final String description = "This is a test stream.";
        final String matchingType = "OR";

        final JsonPath response = createStreamFromRequest(jsonResourceForMethod())
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount+1);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("title", equalTo(streamTitle))
                .body("disabled", equalTo(true))
                .body("matching_type", equalTo(matchingType))
                .body("description", equalTo(description));
    }

    @Test
    public void createFullStreamIncludingStreamrules() {
        final int beforeCount = streamCount();
        final String streamTitle = "A full Test Stream";
        final String description = "This is a full test stream including stream rules.";

        final JsonPath response = createStreamFromRequest(jsonResourceForMethod())
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount + 1);

        final JsonPath getResponse = given()
                .when()
                .get("/streams/" + streamId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .assertThat()
                .body("title", equalTo(streamTitle))
                .body("disabled", equalTo(true))
                .body("matching_type", equalTo("AND"))
                .body("description", equalTo(description))
                .extract().jsonPath();

        assertThat(getResponse.getList("rules")).isNotNull();
        assertThat(getResponse.getList("rules").size()).isEqualTo(2);
    }

    @Test
    public void createFullStreamIncludingInvalidStreamrulesShouldFail() {
        final int beforeCount = streamCount();

        createStreamFromRequest(jsonResourceForMethod())
                .statusCode(400);

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    public void creatingIncompleteStreamShouldFail() throws Exception {
        final int beforeCount = streamCount();

        final ValidatableResponse response = createStreamFromRequest(jsonResourceForMethod());
        response.statusCode(400);

        final int afterCount = streamCount();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    public void creatingStreamWithoutIndexSetIdShouldFail() throws Exception {
        final int beforeCount = streamCount();

        final ValidatableResponse response = createStreamFromRequest(jsonResourceForMethod());

        response.statusCode(400);

        assertThat(streamCount()).isEqualTo(beforeCount);
    }

    @Test
    public void creatingInvalidStreamShouldFail() throws Exception {
        final int beforeCount = streamCount();

        final ValidatableResponse response = createStreamFromRequest("{}");
        response.statusCode(400);

        final int afterCount = streamCount();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    public void creatingInvalidMatchingStreamShouldFail() throws Exception {
        final int beforeCount = streamCount();

        createStreamFromRequest(jsonResourceForMethod())
                .statusCode(400);

        final int afterCount = streamCount();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    @MongoDbSeed(locations = {"single-stream"})
    public void deletingSingleStream() {
        final String streamId = "552b92b2e4b0c055e41ffb8e";
        assertThat(streamCount()).isEqualTo(1);

        given()
                .when()
                .delete("/streams/"+streamId)
                .then()
                .statusCode(204);

        assertThat(streamCount()).isEqualTo(0);

        getStream(streamId)
                .statusCode(404);
    }

    @Test
    @MongoDbSeed(locations = {"single-stream"})
    public void deletingNonexistentStreamShouldFail() {
        final String streamId = "552b92b2e4b0c055e41ffb8f";
        assertThat(streamCount()).isEqualTo(1);

        getStream(streamId)
                .statusCode(404);

        assertThat(streamCount()).isEqualTo(1);
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void updatingTitleOfSingleStream() {
        // id of stream to be updated
        final String streamId = "552b92b2e4b0c055e41ffb8d";
        // id of stream that is supposed to be left untampered
        final String otherStreamId = "552b92b2e4b0c055e41ffb8e";

        assertThat(streamCount()).isEqualTo(2);

        final JsonPath response = given()
                .when()
                .body(jsonResourceForMethod())
                .put("/streams/" + streamId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath();

        assertThat(response.getString("title")).isEqualTo("Updated Title");
        assertThat(response.getString("description")).isEqualTo("This is yet another stream");

        final JsonPath getResponse = given()
                .when()
                .get("/streams/" + streamId)
                .then()
                .statusCode(200)
                .extract().jsonPath();

        assertThat(getResponse.getString("title")).isEqualTo("Updated Title");
        assertThat(getResponse.getString("description")).isEqualTo("This is yet another stream");

        final JsonPath otherStreamResponse = given()
                .when()
                .get("/streams/" + otherStreamId)
                .then()
                .statusCode(200)
                .extract().jsonPath();

        assertThat(otherStreamResponse.getString("title")).isEqualTo("Just a stream");
        assertThat(otherStreamResponse.getString("description")).isEqualTo("This is just a stream");
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void updatingNonexistendStreamShouldFail() {
        // id of nonexistent stream to be updated
        final String streamId = "55affeaffeaffeaffeaffeaf";
        // id of stream that is supposed to be left untampered
        final String otherStreamId = "552b92b2e4b0c055e41ffb8e";

        given()
                .when()
                .body("{\"title\":\"foo\", \"description\":\"bar\", \"index_set_id\":\"582f3c4e43761335e4859f72\"}")
                .put("/streams/" + streamId)
                .then()
                .statusCode(404);

        final JsonPath otherStreamResponse = given()
                .when()
                .get("/streams/" + otherStreamId)
                .then()
                .statusCode(200)
                .extract().jsonPath();

        assertThat(otherStreamResponse.getString("title")).isEqualTo("Just a stream");
        assertThat(otherStreamResponse.getString("description")).isEqualTo("This is just a stream");
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void pausingStream() {
        final String streamId = "552b92b2e4b0c055e41ffb8d";

        given()
                .when()
                .post("/streams/"+streamId+"/pause")
                .then()
                .statusCode(204);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("disabled", equalTo(true));
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void pausingAlreadyPausedStreamShouldNotChangeIt() {
        final String streamId = "552b92b2e4b0c055e41ffb8e";

        given()
                .when()
                .post("/streams/"+streamId+"/pause")
                .then()
                .statusCode(204);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("disabled", equalTo(true));
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void pausingNonexistentStreamShouldFail() {
        final String streamId = "55affeaffeaffeaffeaffeaf";

        given()
                .when()
                .post("/streams/" + streamId + "/pause")
                .then()
                .statusCode(404);
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void resumingStream() {
        final String streamId = "552b92b2e4b0c055e41ffb8e";

        given()
                .when()
                .post("/streams/"+streamId+"/resume")
                .then()
                .statusCode(204);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("disabled", equalTo(false));
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void resumingRunningStreamShouldNotChangeIt() {
        final String streamId = "552b92b2e4b0c055e41ffb8d";

        given()
                .when()
                .post("/streams/"+streamId+"/resume")
                .then()
                .statusCode(204);

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("disabled", equalTo(false));
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void resumingNonexistentStreamShouldFail() {
        final String streamId = "55affeaffeaffeaffeaffeaf";

        given()
                .when()
                .post("/streams/" + streamId + "/resume")
                .then()
                .statusCode(404);
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void updatingMatchingTypeOfStream() {
        // id of stream to be updated
        final String streamId = "552b92b2e4b0c055e41ffb8d";
        // id of stream that is supposed to be left untampered
        final String otherStreamId = "552b92b2e4b0c055e41ffb8e";

        assertThat(streamCount()).isEqualTo(2);

        final JsonPath response = given()
                .when()
                .body("{\"matching_type\":\"OR\"}")
                .put("/streams/" + streamId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().jsonPath();

        assertThat(response.getString("matching_type")).isEqualTo("OR");

        getStream(streamId)
                .statusCode(200)
                .assertThat()
                .body("matching_type", equalTo("OR"));

        getStream(otherStreamId)
                .statusCode(200)
                .assertThat()
                .body("matching_type", equalTo("AND"));
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void updatingUsingInvalidMatchingTypeOfStreamShouldFail() {
        final String streamId = "552b92b2e4b0c055e41ffb8d";

        given()
                .when()
                .body("{\"matching_type\":\"INVALID\"}")
                .put("/streams/" + streamId)
                .then()
                .statusCode(400);

        final JsonPath getResponse = getStream(streamId)
                .statusCode(200)
                .extract().jsonPath();

        assertThat(getResponse.getString("matching_type")).isEqualTo("AND");
    }

    @Test
    @MongoDbSeed(locations = {"single-stream", "second-single-stream"})
    public void updatingMatchingTypeOfNonexistingStreamShouldFail() {
        final String streamId = "552b92b2e4b0deadbeefaffe";
        final String otherStreamId1 = "552b92b2e4b0c055e41ffb8e";
        final String otherStreamId2 = "552b92b2e4b0c055e41ffb8d";

        given()
                .when()
                .body("{\"matching_type\":\"OR\"}")
                .put("/streams/" + streamId)
                .then()
                .statusCode(404);

        // Verify that existing streams are not changed
        getStream(otherStreamId1)
                .statusCode(200)
                .assertThat()
                .body("matching_type", equalTo("AND"));

        getStream(otherStreamId2)
                .statusCode(200)
                .assertThat()
                .body("matching_type", equalTo("AND"));
    }

    @Test
    @MongoDbSeed(locations = "single-stream-with-rules")
    public void testingMatchAndStreamRules() {
        final String streamId = "552b92b2e4b0c055e41ffb8f";

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-both-rules.json"))
                .assertThat()
                .body("matches", equalTo(true))
                .body("rules.55f6b285bee8968146a18911", equalTo(true))
                .body("rules.55f6b28bbee8968146a18918", equalTo(true));

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-one-rule.json"))
                .assertThat()
                .body("matches", equalTo(false))
                .body("rules.55f6b285bee8968146a18911", equalTo(false))
                .body("rules.55f6b28bbee8968146a18918", equalTo(true));

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-no-rules.json"))
                .assertThat()
                .body("matches", equalTo(false))
                .body("rules.55f6b285bee8968146a18911", equalTo(false))
                .body("rules.55f6b28bbee8968146a18918", equalTo(false));
    }

    @Test
    @MongoDbSeed(locations = "single-or-stream-with-rules")
    public void testingMatchOrStreamRules() {
        final String streamId = "552b92b2e4b0c055e41ffb8f";

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-both-rules.json"))
                .assertThat()
                .body("matches", equalTo(true))
                .body("rules.55f6b285bee8968146a18911", equalTo(true))
                .body("rules.55f6b28bbee8968146a18918", equalTo(true));

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-one-rule.json"))
                .assertThat()
                .body("matches", equalTo(true))
                .body("rules.55f6b285bee8968146a18911", equalTo(false))
                .body("rules.55f6b28bbee8968146a18918", equalTo(true));

        streamTestMatch(streamId, jsonResource("testMatch-message-matching-no-rules.json"))
                .assertThat()
                .body("matches", equalTo(false))
                .body("rules.55f6b285bee8968146a18911", equalTo(false))
                .body("rules.55f6b28bbee8968146a18918", equalTo(false));
    }

    protected ValidatableResponse createStreamFromRequest(String request) {
        return given()
                .when()
                .body(request)
                .post("/streams")
                .then()
                .contentType(ContentType.JSON)
                .assertThat();
    }

    protected int streamCount() {
        final JsonPath response =  given()
                .when()
                .get("/streams")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .assertThat()
                .body(".", containsAllKeys("total", "streams"))
                .extract().jsonPath();

        return response.getInt("total");
    }

    protected ValidatableResponse getStream(String streamId) {
        return given()
                .when()
                .get("/streams/" + streamId)
                .then();
    }

    protected ValidatableResponse streamTestMatch(String streamId, String request) {
        return given()
                .when()
                .body(request)
                .post("/streams/" + streamId + "/testMatch")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200);
    }
}
