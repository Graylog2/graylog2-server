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
}
