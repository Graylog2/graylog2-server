package integration;

import com.github.zafarkhaja.semver.Version;
import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiredVersionRule implements MethodRule {
    private static final Logger log = LoggerFactory.getLogger(RequiredVersionRule.class);
    private final RestAssuredSetupRule restAssuredSetupRule;

    public RequiredVersionRule(RestAssuredSetupRule restAssuredSetupRule) {
        this.restAssuredSetupRule = restAssuredSetupRule;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        log.trace("Running version check hook on test {}", method.getMethod());

        final Version serverUnderTestVersion = restAssuredSetupRule.getServerUnderTestVersion();

        if (serverUnderTestVersion == null) {
            throw new NullPointerException("Server version is null!");
        }

        final RequiresVersion methodAnnotation = method.getMethod().getAnnotation(RequiresVersion.class);
        final RequiresVersion classAnnotation = method.getDeclaringClass().getAnnotation(RequiresVersion.class);
        String versionRequirement = null;
        if (classAnnotation != null) {
            versionRequirement = classAnnotation.value();
        } else if (methodAnnotation != null) {
            versionRequirement = methodAnnotation.value();
        }

        log.debug("Checking {} against version {}: ", versionRequirement, serverUnderTestVersion, versionRequirement != null ? serverUnderTestVersion.satisfies(versionRequirement) : "skipped");
        if (versionRequirement != null && !serverUnderTestVersion.satisfies(versionRequirement)) {

            log.warn("Skipping test <{}> because its version requirement <{}> does not meet the server's API version {}",
                    method.getName(), versionRequirement, serverUnderTestVersion);
            return new IgnoreStatement("API Version " + serverUnderTestVersion +
                    " does not meet test requirement " + versionRequirement);
        } else {
            return base;
        }
    }

    class IgnoreStatement extends Statement {
        private final String message;

        public IgnoreStatement(String message) {
            this.message = message;
        }

        @Override
        public void evaluate() throws Throwable {
            Assume.assumeTrue(this.message, false);
        }
    }
}
