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
import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.config.MatcherConfig;
import com.jayway.restassured.matcher.ResponseAwareMatcher;
import com.jayway.restassured.response.Response;
import integration.util.graylog.GraylogControl;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;
import static com.jayway.restassured.http.ContentType.JSON;

@Listeners({BaseRestTest.class, SeedListener.class})
public class BaseRestTest implements IHookable {
    private static final Logger log = LoggerFactory.getLogger(BaseRestTest.class);
    private static Version serverUnderTestVersion;

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

        GraylogControl graylogController = new GraylogControl();
        URL url = graylogController.getServerUrl();
        RestAssured.baseURI = url.getProtocol() + "://" + url.getHost();
        RestAssured.port = url.getPort();
        String[] userInfo = url.getUserInfo().split(":");
        RestAssured.authentication = preemptive().basic(userInfo[0], userInfo[1]);

        // we want all the details for failed tests
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // we want to be able to describe mismatches in a custom way.
        final MatcherConfig matcherConfig = RestAssured.config().getMatcherConfig().errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST);
        RestAssured.config = RestAssured.config().matcherConfig(matcherConfig);

        // we usually send and receive JSON, so we preconfigure for that.
        RestAssured.requestSpecification = new RequestSpecBuilder().build().accept(JSON).contentType(JSON);

        // immediately query the server for its version number, we need it to be able to process the RequireVersion annotations.
        given().when()
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
        if (serverUnderTestVersion == null) {
            throw new RuntimeException("");
        }
    }

    /**
     * Use this method to load a json file from the classpath.
     * <p>
     *     It will be looked for relative to the caller's object's class.
     * </p>
     * For example if you have a class in the package <code>integration.system.users</code> and call <code>jsonResource("abc.json")</code>
     * on the instance of your test class (i.e. <code>this</code>), it will look for a file in <code>resources/integration/system/inputs/abc.json</code>.
     * <p>
     *     If the file does not exist or cannot be read, a {@link java.lang.IllegalStateException} will be raised which should cause your test to fail.
     * </p>
     * @param relativeFileName the name of the file relative to the caller's class.
     * @return the bytes in that file.
     */
    protected byte[] jsonResource(String relativeFileName) {
        final URL resource = Resources.getResource(this.getClass(), relativeFileName);
        if (resource == null) {
            throw new IllegalStateException("Unable to find JSON resource " + relativeFileName + " for test. This is a bug.");
        }
        try {
            return Resources.toByteArray(resource);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON resource " + relativeFileName + " for test. This is a bug.");
        }
    }

    /**
     * Same as @{link #jsonResource} but guesses the file name from the caller's method name. Be careful when refactoring.
     * @return the bytes in that file
     */
    protected byte[] jsonResourceForMethod() {
        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        final String testMethodName = stackTraceElements[2].getMethodName();

        final String filename = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, testMethodName);

        return jsonResource(filename + ".json");
    }

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        log.trace("Running version check hook on test {}", testResult.getMethod());

        final RequiresVersion methodAnnotation = testResult.getMethod().getConstructorOrMethod()
                .getMethod().getAnnotation(RequiresVersion.class);
        final RequiresVersion classAnnotation = (RequiresVersion) testResult.getTestClass().getRealClass().getAnnotation(RequiresVersion.class);
        String versionRequirement = null;
        if (classAnnotation != null) {
            versionRequirement = classAnnotation.value();
        } else if (methodAnnotation != null) {
            versionRequirement = methodAnnotation.value();
        }

        log.info("Checking {} against version {}: ", versionRequirement, serverUnderTestVersion, versionRequirement != null ? serverUnderTestVersion.satisfies(versionRequirement) : "skipped");
        if (serverUnderTestVersion != null && versionRequirement != null && !serverUnderTestVersion.satisfies(versionRequirement)) {

            log.info("Skipping test <{}> because its version requirement <{}> does not meet the server's API version {}",
                     testResult.getName(), versionRequirement, serverUnderTestVersion);
            throw new SkipException("API Version " + serverUnderTestVersion +
                                            " does not meet test requirement " + versionRequirement);
        }
        callBack.runTestMethod(testResult);
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
