package integration;

import com.github.zafarkhaja.semver.Version;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.config.MatcherConfig;
import integration.util.graylog.GraylogControl;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.rules.ExternalResource;

import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;
import static com.jayway.restassured.http.ContentType.JSON;

public class RestAssuredSetupRule extends ExternalResource {
    private static Version serverUnderTestVersion;

    @Override
    protected void before() throws Throwable {
        if (System.getProperty("gl2.integration.tests") == null) {
            // TODO: properly skip execution
            throw new RuntimeException("Not running REST API integration tests. Add -Dgl2.integration.tests to run them.");
        }

        final GraylogControl graylogController = new GraylogControl();
        final URL url = graylogController.getUrl();
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
    }

    public Version getServerUnderTestVersion() {
        return serverUnderTestVersion;
    }
}
