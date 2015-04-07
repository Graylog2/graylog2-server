package integration.system.agents;

import integration.BaseRestTest;
import integration.RequiresVersion;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.jodatime.api.Assertions.assertThat;

@RequiresVersion(">=1.1.0")
public class AgentsTest extends BaseRestTest {
    private final String resourcePrefix = "/system/agents";
    private final String resourceEndpoint = resourcePrefix + "/register";

    @Test
    public void testRegisterAgent() throws Exception {
        given().when()
                    .body(jsonResourceForMethod())
                    .post(resourceEndpoint)
                .then()
                    .statusCode(202);
    }

    @Test
    public void testRegisterInvalidAgent() throws Exception {
        given().when()
                    .body(jsonResourceForMethod())
                    .post(resourceEndpoint)
                .then()
                    .statusCode(400);
    }

    @Test
    public void testListAgents() throws Exception {
        given().when()
                    .get(resourcePrefix)
                .then()
                    .statusCode(200)
                    .assertThat().body("agents", notNullValue());
    }

    @Test
    public void testGetAgent() throws Exception {
        given().when()
                    .body(jsonResourceForMethod())
                    .post(resourceEndpoint)
                .then()
                    .statusCode(202);

        given().when()
                .get(resourcePrefix + "/getAgentTest")
                .then()
                    .statusCode(200)
                    .assertThat()
                        .body("id", is("getAgentTest"))
                        .body(".", containsAllKeys("id", "node_id", "node_details", "last_seen", "active"))
                        .body("active", is(true));
    }

    @Test
    public void testTouchAgent() throws Exception {
        final String agentId = "testTouchAgentId";

        given().when()
                .body(jsonResourceForMethod())
                    .post(resourceEndpoint)
                .then()
                    .statusCode(202);

        final DateTime lastSeenBefore = getLastSeenForAgentId(agentId);

        given().when()
                .body(jsonResource("test-register-agent.json"))
                .post(resourceEndpoint)
                .then()
                .statusCode(202);

        final DateTime lastSeenAfterOtherRegistration = getLastSeenForAgentId(agentId);

        given().when()
                .body(jsonResourceForMethod())
                .post(resourceEndpoint)
                .then()
                .statusCode(202);

        final DateTime lastSeenAfter = getLastSeenForAgentId(agentId);

        assertThat(lastSeenBefore).isEqualTo(lastSeenAfterOtherRegistration);
        assertThat(lastSeenBefore)
                .isNotEqualTo(lastSeenAfter)
                .isBefore(lastSeenAfter);
    }

    private DateTime getLastSeenForAgentId(String agentId) {
        final String content = get(resourcePrefix).asString();
        final List<String> lastSeenStringsBefore = from(content).get("agents.findAll { agent -> agent.id == \"" + agentId + "\" }.last_seen");
        assertThat(lastSeenStringsBefore).isNotEmpty().hasSize(1);

        return DateTime.parse(lastSeenStringsBefore.get(0));
    }
}
