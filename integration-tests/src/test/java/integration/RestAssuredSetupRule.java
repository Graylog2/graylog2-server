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
package integration;

import com.github.zafarkhaja.semver.Version;
import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.MatcherConfig;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.rules.ExternalResource;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.preemptive;
import static io.restassured.http.ContentType.JSON;

public class RestAssuredSetupRule extends ExternalResource {
    private static Version serverUnderTestVersion;
    private AuthenticationScheme authenticationScheme;

    @Override
    protected void before() throws Throwable {
        final URI uri = IntegrationTestsConfig.getGlServerURL();
        RestAssured.baseURI = uri.getScheme() + "://" + uri.getHost();
        RestAssured.port = uri.getPort();
        String[] userInfo = uri.getUserInfo().split(":");
        authenticationScheme = preemptive().basic(userInfo[0], userInfo[1]);

        // we want all the details for failed tests
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // we want to be able to describe mismatches in a custom way.
        final MatcherConfig matcherConfig = RestAssured.config().getMatcherConfig().errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST);
        RestAssured.config = RestAssured.config().matcherConfig(matcherConfig);

        // we usually send and receive JSON, so we preconfigure for that.
        RestAssured.requestSpecification = new RequestSpecBuilder().build().accept(JSON).contentType(JSON);

        // immediately query the server for its version number, we need it to be able to process the RequireVersion annotations.
        given().when()
                .auth().basic(userInfo[0], userInfo[1])
                    .get("/system")
                .then()
                    .body("version", new BaseMatcher<Object>() {
                        @Override
                        public boolean matches(Object item) {
                            if (item instanceof String) {
                                String str = (String) item;
                                try {
                                    // clean our slightly non-semver version number
                                    str = str.replaceAll("\\(.*?\\)", "").trim();
                                    serverUnderTestVersion = Version.valueOf(str);
                                    return true;
                                } catch (RuntimeException e) {
                                    return false;
                                }
                            }
                            return false;
                        }

                        @Override
                        public void describeTo(Description description) {
                            description.appendText("parse the system under test's version number");
                        }
                    });
    }

    public Version getServerUnderTestVersion() {
        return serverUnderTestVersion;
    }

    public AuthenticationScheme getAuthenticationScheme() {
        return authenticationScheme;
    }
}
