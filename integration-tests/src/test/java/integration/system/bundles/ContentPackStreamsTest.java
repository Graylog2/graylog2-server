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
package integration.system.bundles;

import com.google.common.net.HttpHeaders;
import integration.BaseRestTest;
import integration.RequiresAuthentication;
import integration.RequiresVersion;
import io.restassured.response.ValidatableResponse;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

@Ignore("legacy test that should be converted or deleted")
@RequiresVersion(">=1.2.0")
@RequiresAuthentication
public class ContentPackStreamsTest extends BaseRestTest {

    @Test
    public void createAndShowAndApplyAndDeleteTest() {
        final URI uri = createContentPackWithStreams();
        showContentPack(uri);
        applyContentPack(uri);
        deleteContentPack(uri);
    }

    private URI createContentPackWithStreams() {
        final ValidatableResponse validatableResponse = given().when()
                .body(jsonResourceForMethod()).post("/system/bundles")
                .then()
                .statusCode(201)
                .statusLine(notNullValue());

        final String locationHeader = validatableResponse.extract().header(HttpHeaders.LOCATION);
        final URI uri = URI.create(locationHeader);

        assertThat(uri.getPath()).startsWith("/system/bundles");

        return uri;
    }

    private void showContentPack(URI uri) {
        given().when()
                .get(uri)
                .then()
                .statusCode(200)
                .statusLine(notNullValue())
                .body(".", containsKeys("id", "name", "description", "category", "streams"));
    }

    private void applyContentPack(URI uri) {
        final URI applyUri = URI.create(uri.toASCIIString() + "/apply");
        given().when()
                .post(applyUri)
                .then()
                .statusCode(204)
                .statusLine(notNullValue());
    }

    private void deleteContentPack(URI uri) {
        given().when()
                .delete(uri)
                .then()
                .statusCode(204);
    }
}
