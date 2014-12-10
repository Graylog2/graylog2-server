/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package integration;

import com.google.common.collect.Sets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.MatcherConfig;
import com.jayway.restassured.matcher.ResponseAwareMatcher;
import com.jayway.restassured.response.Response;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.jayway.restassured.RestAssured.preemptive;

public class BaseRestTest {
    /**
     * Returns a {@link com.jayway.restassured.matcher.ResponseAwareMatcher} which checks that all given keys are present,
     * and that no additional keys are in the given path.
     * Given this JSON
     * <pre>
     *     {
     *      "version": "2.0.0",
     *      "codename": "foo"
     *     }
     * </pre>
     * to validate that it contains the keys <code>version</code> and <code>codename</code> and only those keys, use
     * <pre>
     *     given()
     *        .when()
     *          .get("/")
     *        .then()
     *          .body(".", containsAllKeys("codename", "version"));
     * </pre>
     * If any of the keys are missing, or there are other keys in the JSON document, the matcher will fail.
     * @param keys the keys that need to be present
     * @return matcher
     */
    public static ResponseAwareMatcher<Response> containsAllKeys(String... keys) {
        return new KeysPresentMatcher(keys);
    }

    @BeforeSuite
    public void setupTestSuite() {
        if (System.getProperty("gl2.integration.tests") == null) {
            throw new SkipException("Not running REST API integration tests. Add -Dgl2.integration.tests to run them.");
        }

        RestAssured.baseURI = System.getProperty("gl2.baseuri", "http://localhost");
        RestAssured.port = Integer.parseInt(System.getProperty("gl2.port", "12900"));
        RestAssured.authentication = preemptive().basic(
                System.getProperty("gl2.admin_user", "admin"),
                System.getProperty("gl2.admin_password", "admin"));

        // we want all the details for failed tests
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        // we want to be able to describe mismatches in a custom way.
        final MatcherConfig matcherConfig = RestAssured.config().getMatcherConfig().errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST);
        RestAssured.config = RestAssured.config().matcherConfig(matcherConfig);
    }

    private static class KeysPresentMatcher extends ResponseAwareMatcher<Response> {
        private final Set<String> keys = Sets.newHashSet();
        public KeysPresentMatcher(String... keys) {
            Collections.addAll(this.keys, keys);
        }

        @Override
        public Matcher<?> matcher(Response response) throws Exception {
            return new BaseMatcher<Response>() {

                private Sets.SetView difference;

                @Override
                public boolean matches(Object item) {
                    if (item instanceof Map) {
                        final Set keySet = ((Map) item).keySet();
                        difference = Sets.symmetricDifference(keySet, keys);
                        return difference.isEmpty();
                    }
                    return false;
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("JSON Contains all keys: ").appendValueList("[", ", ", "]", keys);
                }

                @Override
                public void describeMismatch(Object item, Description description) {
                    super.describeMismatch(item, description);
                    description.appendValueList(" has extra or missing keys: [", ", ", "]", difference);
                }
            };
        }
    }
}
