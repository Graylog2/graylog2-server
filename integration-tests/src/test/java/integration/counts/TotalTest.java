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
package integration.counts;

import integration.BaseRestTest;
import integration.RequiresAuthentication;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

@Ignore("legacy test that should be converted or deleted")
@RequiresAuthentication
public class TotalTest extends BaseRestTest {
    @Test
    public void total() {
        given()
                .when()
                .get("/count/total")
                .then()
                .statusCode(200)
                .body(".", containsAllKeys("events"));
    }
}
