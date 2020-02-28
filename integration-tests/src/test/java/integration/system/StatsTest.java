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
package integration.system;

import integration.BaseRestTest;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

@Ignore("legacy test that should be converted or deleted")
@RequiresVersion(">=1.1.0")
@RequiresAuthentication
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
