package integration;

import com.jayway.restassured.RestAssured;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiresAuthenticationRule implements MethodRule {
    private static final Logger log = LoggerFactory.getLogger(RequiredVersionRule.class);
    private final RestAssuredSetupRule restAssuredSetupRule;

    public RequiresAuthenticationRule(RestAssuredSetupRule restAssuredSetupRule) {
        this.restAssuredSetupRule = restAssuredSetupRule;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        if (method.getAnnotation(RequiresAuthentication.class) != null
                ||  method.getDeclaringClass().getAnnotation(RequiresAuthentication.class) != null) {
            log.debug("Enabling authentication for method " + method.getName());
            RestAssured.authentication = restAssuredSetupRule.getAuthenticationScheme();
        } else {
            log.debug("Disabling authentication for method " + method.getName());
            RestAssured.authentication = RestAssured.DEFAULT_AUTH;
        }

        return base;
    }
}
