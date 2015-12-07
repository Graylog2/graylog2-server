package integration.roles;

import integration.BaseRestTest;
import integration.RequiresAuthentication;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

@RequiresAuthentication
public class RolesResourceTest extends BaseRestTest {

    @Test
    public void deleteRolesTest() {
        try {
            given().when()
                    .body(jsonResource("create-role.json")).post("/roles")
                    .then()
                    .statusCode(201);

            given().when()
                    .body(jsonResource("create-user-with-role.json")).post("/users")
                    .then()
                    .statusCode(201);

            given().when()
                    .get("/users/{name}", "john")
                    .then()
                    .statusCode(200)
                    .body("roles", setContainsEntry("Test"));

            given().when()
                    .delete("/roles/{name}", "Test")
                    .then()
                    .statusCode(204);

            given().when()
                    .get("/users/{name}", "john")
                    .then()
                    .statusCode(200)
                    .body("roles", setDoesNotContain("Test"));
        } finally {
            // still remove the user even though it's not technically necessary
            given().when()
                    .delete("/users/{name}", "john")
                    .then()
                    .statusCode(204);
        }
    }
}
