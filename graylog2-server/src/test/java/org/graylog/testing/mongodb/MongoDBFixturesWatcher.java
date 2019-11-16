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
package org.graylog.testing.mongodb;

import com.google.common.io.Resources;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MongoDBFixturesWatcher extends TestWatcher {
    private String[] resourceNames;

    @Override
    protected void starting(final Description description) {
        final MongoDBFixtures fixtures = description.getAnnotation(MongoDBFixtures.class);
        this.resourceNames = fixtures != null ? fixtures.value() : new String[]{};
    }

    List<URL> fixtureResources(final Class<?> contextClass) {
        return Arrays.stream(resourceNames).map(resourceName -> toResource(resourceName, contextClass)).collect(Collectors.toList());
    }

    private URL toResource(final String resourceName, final Class<?> contextClass) {
        if (Paths.get(resourceName).getNameCount() > 1) {
            return Resources.getResource(resourceName);
        } else {
            return Resources.getResource(contextClass, resourceName);
        }
    }
}
