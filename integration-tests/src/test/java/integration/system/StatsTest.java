package integration.system;

import integration.BaseRestTest;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.*;

@RequiresVersion(">=1.1.0")
public class StatsTest extends BaseRestTest {
    private static final String resourcePrefix = "/system/stats";

    @Test
    public void testSystemStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix)
                .then()
                    .statusCode(200)
                    .assertThat()
                        .body(".", containsAllKeys("fs", "jvm", "network", "os", "process"));
    }

    @Test
    public void testFsStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix + "/fs")
                .then()
                    .statusCode(200)
                    .assertThat()
                        .body(".", containsAllKeys("filesystems"));

    }

    @Test
    public void testJvmStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix + "/jvm")
                .then()
                    .statusCode(200);
    }

    @Test
    public void testNetworkStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix + "/network")
                .then()
                    .statusCode(200);
    }

    @Test
    public void testOsStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix + "/os")
                .then()
                    .statusCode(200);
    }

    @Test
    public void testProcessStats() throws Exception {
        given()
                .when()
                    .get(resourcePrefix + "/process")
                .then()
                    .statusCode(200);
    }
}