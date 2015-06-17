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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RunIfProperty implements TestRule {
    private final String propertyName;

    public RunIfProperty(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        final String propertyValue = System.getProperty(this.propertyName);
        if (propertyValue == null || !Boolean.valueOf(propertyValue)) {
            return new IgnoreStatement("Not running REST API integration tests. Add -Dgl2.integration.tests to run them.");
        } else {
            return base;
        }
    }
}
