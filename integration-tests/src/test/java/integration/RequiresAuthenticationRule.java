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

import io.restassured.RestAssured;
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
