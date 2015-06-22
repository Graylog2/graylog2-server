package integration.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.ValidatableResponse;
import integration.BaseRestTest;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import org.junit.Test;

import javax.annotation.Nullable;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RequiresAuthentication
@RequiresVersion(">=0.90.0")
public class StreamsTest extends BaseRestTest {
    @AutoValue
    @JsonAutoDetect
    abstract static class SimpleCreateStreamRequest {
        @JsonProperty @Nullable public abstract String title();

        public static SimpleCreateStreamRequest create(String title) {
            return new AutoValue_StreamsTest_SimpleCreateStreamRequest(title);
        }
    }

    @AutoValue
    @JsonAutoDetect
    abstract static class CreateStreamRequest {
        @JsonProperty public abstract String title();
        @JsonProperty public abstract String description();

        public static CreateStreamRequest create(String title, String description) {
            return new AutoValue_StreamsTest_CreateStreamRequest(title, description);
        }
    }

    @Test
    public void testListStreams() throws Exception {
        given()
            .when()
                .get("/streams")
            .then()
                .statusCode(200)
                .assertThat()
                .body(".", containsAllKeys("total", "streams"));
    }

    @Test
    public void testSimpleCreateStream() throws Exception {
        final int beforeCount = streamCount();
        final String streamTitle = "TestStream";

        final JsonPath response = createStreamFromRequest(SimpleCreateStreamRequest.create(streamTitle))
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount+1);

        given()
            .when()
                .get("/streams/" + streamId)
            .then()
                .statusCode(200)
                .assertThat()
                .body("title", equalTo(streamTitle))
                .body("disabled", equalTo(true))
                .body("description", equalTo(null));
    }

    @Test
    public void testCreateStream() throws Exception {
        final int beforeCount = streamCount();
        final String streamTitle = "Another Test Stream";
        final String description = "This is a test stream.";

        final JsonPath response = createStreamFromRequest(CreateStreamRequest.create(streamTitle, description))
                .statusCode(201)
                .body(".", containsAllKeys("stream_id"))
                .extract().jsonPath();

        final String streamId = response.getString("stream_id");

        assertThat(streamId).isNotNull().isNotEmpty();

        final int afterCount = streamCount();

        assertThat(afterCount).isEqualTo(beforeCount+1);

        given()
            .when()
                .get("/streams/" + streamId)
            .then()
                .statusCode(200)
                .assertThat()
                .body("title", equalTo(streamTitle))
                .body("disabled", equalTo(true))
                .body("description", equalTo(description));
    }

    @Test
    public void testIncompleteStream() throws Exception {
        final int beforeCount = streamCount();

        final ValidatableResponse response = createStreamFromRequest(SimpleCreateStreamRequest.create(null));
        response.statusCode(400);

        final int afterCount = streamCount();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    @Test
    public void testInvalidStream() throws Exception {
        final int beforeCount = streamCount();

        final ValidatableResponse response = createStreamFromRequest("{}");
        response.statusCode(400);

        final int afterCount = streamCount();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    protected ValidatableResponse createStreamFromRequest(Object request) {
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
}
