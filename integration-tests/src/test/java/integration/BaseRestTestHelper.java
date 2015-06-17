package integration;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.restassured.matcher.ResponseAwareMatcher;
import com.jayway.restassured.response.Response;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BaseRestTestHelper {
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
